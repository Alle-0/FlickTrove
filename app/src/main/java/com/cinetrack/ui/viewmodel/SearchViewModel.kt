package com.cinetrack.ui.viewmodel

import com.cinetrack.R
import com.cinetrack.ui.utils.UiText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinetrack.data.Movie
import com.cinetrack.data.repository.MovieRepository
import com.cinetrack.domain.CycleMovieStatusUseCase
import com.cinetrack.data.api.TMDBSearchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import com.cinetrack.data.models.SortConfig
import com.cinetrack.data.api.TMDBService
import com.cinetrack.ui.utils.ErrorMapper
import com.cinetrack.ui.utils.ActionFeedbackManager
import javax.inject.Inject
import javax.inject.Named
import com.cinetrack.domain.applyFilter
import com.cinetrack.data.local.entities.FolderEntity
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap

data class FilterPill(val id: Long, val name: String, val isKeyword: Boolean = false)

data class SearchUiState(
    val query: String = "",
    val category: String = "movie",
    val results: ImmutableList<TMDBSearchResult> = persistentListOf(),
    val trendingMovies: ImmutableList<TMDBSearchResult> = persistentListOf(),
    val trendingTv: ImmutableList<TMDBSearchResult> = persistentListOf(),
    val trendingPeople: ImmutableList<TMDBSearchResult> = persistentListOf(),
    val isLoading: Boolean = false,
    val isNextPageLoading: Boolean = false,
    val isEndReached: Boolean = false,
    val favorites: ImmutableList<Movie> = persistentListOf(),
    val folders: ImmutableList<FolderEntity> = persistentListOf(),
    val recentSearches: ImmutableList<String> = persistentListOf(),
    val sortConfig: SortConfig = SortConfig(sortType = "popularity"),
    val movieFolderColors: ImmutableMap<String, ImmutableList<String>> = persistentMapOf(),
    val preferences: com.cinetrack.data.models.UserPreferences = com.cinetrack.data.models.UserPreferences(),
    val togglingIds: PersistentSet<Long> = persistentSetOf(),
    val errorMessage: String? = null,
    val suggestedFilters: ImmutableList<FilterPill> = persistentListOf()
)

@OptIn(kotlinx.coroutines.FlowPreview::class)
@dagger.hilt.android.lifecycle.HiltViewModel
class SearchViewModel @Inject constructor(
    private val cycleMovieStatusUseCase: CycleMovieStatusUseCase,
    private val getSearchUiStateUseCase: com.cinetrack.domain.GetSearchUiStateUseCase,
    private val repository: MovieRepository,
    private val preferenceRepository: com.cinetrack.data.repository.PreferenceRepository,
    private val tmdbService: TMDBService,
    private val actionFeedbackManager: ActionFeedbackManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    
    private var rawResults: List<TMDBSearchResult> = emptyList()
    private var rawTrendingMovies: List<TMDBSearchResult> = emptyList()
    private var rawTrendingTv: List<TMDBSearchResult> = emptyList()
    private var rawTrendingPeople: List<TMDBSearchResult> = emptyList()
    private var rawDynamicKeywords: List<FilterPill> = emptyList()
    
    private var localMovies: List<Movie> = emptyList()
    private var localFolders: List<FolderEntity> = emptyList()
    
    val lazyGridState = androidx.compose.foundation.lazy.grid.LazyGridState()
    val animatedMovieIds = mutableSetOf<String>()
    
    private fun applyStateFilters() {
        _uiState.update { currentState ->
            getSearchUiStateUseCase(
                currentState = currentState,
                rawResults = rawResults,
                rawTrendingMovies = rawTrendingMovies,
                rawTrendingTv = rawTrendingTv,
                rawTrendingPeople = rawTrendingPeople,
                rawDynamicKeywords = rawDynamicKeywords,
                localMovies = localMovies,
                folders = localFolders
            )
        }
    }
    private var currentPage = 1
    
    fun emitMessage(message: UiText) {
        actionFeedbackManager.emit(message)
    }
    
    private var searchJob: Job? = null
    private var keywordSearchJob: Job? = null
    
    init {
        // Initial fetch for trending or discovery
        performSearch()
 

        viewModelScope.launch {
            repository.getLocalMoviesFlow().collect { movies ->
                localMovies = movies
                applyStateFilters()
            }
        }
        viewModelScope.launch {
            repository.getFoldersFlow().collect { folders ->
                localFolders = folders
                applyStateFilters()
            }
        }
        viewModelScope.launch {
            repository.getRecentSearches().collect { recent ->
                _uiState.update { it.copy(recentSearches = recent.toImmutableList()) }
            }
        }
        viewModelScope.launch {
            preferenceRepository.userPreferencesFlow.collect { prefs ->
                _uiState.update { it.copy(preferences = prefs) }
            }
        }

        viewModelScope.launch {
            _uiState.map { it.query }
                .debounce { q -> if (q.isEmpty()) 0L else 500L }
                .distinctUntilChanged()
                .collect { performSearch() }
        }
        viewModelScope.launch {
            _uiState.map { it.category to it.sortConfig }
                .drop(1)
                .distinctUntilChanged()
                .collect { performSearch() }
        }
    }


    fun onQueryChanged(newQuery: String) {
        if (_uiState.value.query == newQuery) return
        _uiState.update { it.copy(query = newQuery) }
        
        if (newQuery.isNotEmpty() && _uiState.value.sortConfig.selectedKeywords.isNotEmpty()) {
            _uiState.update { it.copy(sortConfig = _uiState.value.sortConfig.copy(selectedKeywords = emptyList())) }
        }
        
        keywordSearchJob?.cancel()
        if (newQuery.length >= 3) {
            keywordSearchJob = viewModelScope.launch {
                kotlinx.coroutines.delay(400)
                try {
                    val response = tmdbService.searchKeyword(query = newQuery)
                    val pills = response.getList().take(6).map { 
                        FilterPill(it.id, it.name.replaceFirstChar { c -> c.uppercase() }, isKeyword = true) 
                    }
                    rawDynamicKeywords = pills
                    applyStateFilters()
                } catch (e: Exception) {
                    if (e !is kotlinx.coroutines.CancellationException) {
                        rawDynamicKeywords = emptyList()
                        applyStateFilters()
                    }
                }
            }
        } else {
            rawDynamicKeywords = emptyList()
            applyStateFilters()
        }
    }

    fun onCategoryChanged(newCategory: String) {
        if (_uiState.value.category == newCategory) return
        _uiState.update { it.copy(category = newCategory) }
        // Reset selected genres on category change to avoid cross-contamination of movie/tv genre IDs
        _uiState.update { it.copy(sortConfig = _uiState.value.sortConfig.copy(selectedGenres = emptyList())) }
    }

    fun updateSortConfig(config: SortConfig) {
        _uiState.update { it.copy(sortConfig = config) }
    }

    fun retry() {
        performSearch()
    }

    private fun performSearch() {
        searchJob?.cancel()
        val query = _uiState.value.query
        val category = _uiState.value.category
        _uiState.update { it.copy(errorMessage = null) }

        if (query.isEmpty()) {
            val config = _uiState.value.sortConfig
            val hasFilters = config.selectedGenres.isNotEmpty() || config.selectedKeywords.isNotEmpty() || config.selectedDecades.isNotEmpty() || config.sortType != "popularity"
            if (hasFilters && category != "person") {
                fetchDiscovery()
            } else {
                rawResults = emptyList()
                applyStateFilters()
                fetchTrending()
            }
            return
        }

        rawResults = emptyList()
        applyStateFilters()

        if (query.length <= 2) {
            _uiState.update { it.copy(isLoading = false) }
            _uiState.update { it.copy(isEndReached = false) }
            currentPage = 1
            return
        }
        _uiState.update { it.copy(isEndReached = false) }
        currentPage = 1

        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                var page = 1
                var accumulatedResults = emptyList<TMDBSearchResult>()
                var reachedEnd = false
                
                while (page <= 15) {
                    val newBatch: List<TMDBSearchResult>
                    var totalPages = 1

                    when (category) {
                        "movie" -> {
                            val response = tmdbService.searchMovie(query, page = page)
                            newBatch = response.results.map { it.toMovieResultInternal() }
                            totalPages = response.totalPages ?: 1
                        }
                        "tv" -> {
                            val response = tmdbService.searchTV(query, page = page)
                            newBatch = response.results.map { it.toTvResultInternal() }
                            totalPages = response.totalPages ?: 1
                        }
                        "person" -> {
                            val response = tmdbService.searchPeople(query, page = page)
                            newBatch = response.results.map { 
                                TMDBSearchResult.PersonResult(it.id, it.name, it.profilePath, it.knownForDepartment)
                            }
                            totalPages = response.totalPages ?: 1
                        }
                        else -> {
                            val response = tmdbService.searchMulti(query, page = page)
                            newBatch = response.results
                            totalPages = response.totalPages ?: 1
                        }
                    }

                    if (newBatch.isEmpty()) {
                        reachedEnd = true
                        break
                    }

                    accumulatedResults = accumulatedResults + newBatch
                    rawResults = accumulatedResults
                    applyStateFilters()
                    currentPage = page
                    
                    if (page >= totalPages) {
                        reachedEnd = true
                        break
                    }

                    val filteredCount = accumulatedResults.applyFilter(_uiState.value.sortConfig).size
                    if (_uiState.value.sortConfig.selectedGenres.isEmpty() || category == "person" || filteredCount >= 40) {
                        break
                    }
                    page++
                }
                
                _uiState.update { it.copy(isEndReached = reachedEnd) }

                if (accumulatedResults.isEmpty() && query.length >= 3) {
                    // FALLBACK: If no results found, try a fuzzy match by using a prefix of the longest word
                    val fallbackQuery = com.cinetrack.ui.utils.FuzzySearch.buildFallbackQuery(query)
                    if (fallbackQuery != null && fallbackQuery != query) {
                        var fallbackPage = 1
                        while (fallbackPage <= 2) {
                            val fallbackBatch: List<TMDBSearchResult> = when (category) {
                                "movie" -> tmdbService.searchMovie(fallbackQuery, page = fallbackPage).results.map { it.toMovieResultInternal() }
                                "tv" -> tmdbService.searchTV(fallbackQuery, page = fallbackPage).results.map { it.toTvResultInternal() }
                                "person" -> tmdbService.searchPeople(fallbackQuery, page = fallbackPage).results.map { TMDBSearchResult.PersonResult(it.id, it.name, it.profilePath, it.knownForDepartment) }
                                else -> tmdbService.searchMulti(fallbackQuery, page = fallbackPage).results
                            }
                            
                            if (fallbackBatch.isEmpty()) break
                            
                            // Score and filter fallback results
                            val scoredFallback = fallbackBatch.filter { res ->
                                com.cinetrack.ui.utils.FuzzySearch.score(query, res.displayTitle) > 0.45
                            }.sortedByDescending { res ->
                                com.cinetrack.ui.utils.FuzzySearch.score(query, res.displayTitle)
                            }
                            
                            if (scoredFallback.isNotEmpty()) {
                                rawResults = accumulatedResults
                                applyStateFilters()
                                break // Found something fuzzy
                            }
                            fallbackPage++
                        }
                    }
                }

                if (accumulatedResults.isNotEmpty() && query.length >= 3) {
                    repository.saveSearchQuery(query)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(isEndReached = true) }
                _uiState.update { it.copy(errorMessage = ErrorMapper.map(e.message)) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
    
    private fun buildTmdbOptions(config: SortConfig, category: String): Map<String, String> {
        val options = mutableMapOf<String, String>()
        if (config.selectedGenres.isNotEmpty()) options["with_genres"] = config.selectedGenres.joinToString("|")
        if (config.selectedKeywords.isNotEmpty()) options["with_keywords"] = config.selectedKeywords.joinToString("|")

        if (config.selectedDecades.isNotEmpty()) {
            val decades = config.selectedDecades.mapNotNull { it.removeSuffix("s").toIntOrNull() }
            if (decades.isNotEmpty()) {
                val minYear = decades.minOrNull() ?: 1900
                val maxYear = (decades.maxOrNull() ?: 2020) + 9
                if (category == "tv") {
                    options["first_air_date.gte"] = "$minYear-01-01"
                    options["first_air_date.lte"] = "$maxYear-12-31"
                } else {
                    options["primary_release_date.gte"] = "$minYear-01-01"
                    options["primary_release_date.lte"] = "$maxYear-12-31"
                }
            }
        }

        val direction = config.sortDirection
        val sortBy = when (config.sortType) {
            "vote_average" -> "vote_average.$direction"
            "release_date" -> if (category == "tv") "first_air_date.$direction" else "primary_release_date.$direction"
            "title" -> if (category == "tv") "name.$direction" else "title.$direction"
            else -> "popularity.$direction"
        }
        options["sort_by"] = sortBy
        if (config.sortType == "vote_average") options["vote_count.gte"] = "100"

        return options
    }

    private fun fetchDiscovery() {
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            _uiState.update { it.copy(isEndReached = false) }
            currentPage = 1
            try {
                val category = _uiState.value.category
                val config = _uiState.value.sortConfig
                
                if (category == "person") {
                    rawResults = emptyList()
                    applyStateFilters()
                    fetchTrending()
                    return@launch
                }

                val hasFilters = config.selectedGenres.isNotEmpty() || config.selectedKeywords.isNotEmpty() || config.selectedDecades.isNotEmpty() || config.sortType != "popularity"

                val results = if (hasFilters) {
                    coroutineScope {
                        val options = buildTmdbOptions(config, category)
                        (1..5).map { pageNum ->
                            async {
                                try {
                                    when (category) {
                                        "movie" -> tmdbService.discoverMovies(page = pageNum, options = options).results.map { it.toMovieResultInternal() }
                                        "tv" -> tmdbService.discoverTV(page = pageNum, options = options).results.map { it.toTvResultInternal() }
                                        else -> emptyList()
                                    }
                                } catch (e: Exception) {
                                    if (e is CancellationException) throw e
                                    emptyList<TMDBSearchResult>()
                                }
                            }
                        }.awaitAll().flatten()
                    }
                } else {
                    when (category) {
                        "movie" -> repository.getTrendingMovies(1).map { it.toMovieResultInternal() }
                        "tv" -> repository.getTrendingTV(1).map { it.toTvResultInternal() }
                        else -> emptyList()
                    }
                }

                rawResults = results
                applyStateFilters()
                currentPage = 5
                _uiState.update { it.copy(isEndReached = results.size < 100) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                rawResults = emptyList()
                applyStateFilters()
                _uiState.update { it.copy(errorMessage = ErrorMapper.map(e.message)) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
    
    private fun fetchTrending() {
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            _uiState.update { it.copy(isEndReached = false) }
            currentPage = 1
            try {
                val movies = repository.getTrendingMovies(1).take(6).map { it.toMovieResultInternal() }
                val tv = repository.getTrendingTV(1).take(6).map { it.toTvResultInternal() }
                val people = repository.getPopularPeople(1).take(6).map { 
                    TMDBSearchResult.PersonResult(it.id, it.name, it.profilePath, it.knownForDepartment)
                }
                
                rawTrendingMovies = movies
                rawTrendingTv = tv
                rawTrendingPeople = people
                rawResults = emptyList()
                applyStateFilters()
                _uiState.update { it.copy(isEndReached = true) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                android.util.Log.e("SearchViewModel", "fetchTrending failed!", e)
                _uiState.update { it.copy(errorMessage = ErrorMapper.map(e.message)) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun loadNextPage() {
        if (_uiState.value.isNextPageLoading || _uiState.value.isEndReached || _uiState.value.isLoading) return
        val query = _uiState.value.query
        val category = _uiState.value.category

        viewModelScope.launch {
            _uiState.update { it.copy(isNextPageLoading = true) }
            try {
                var page = currentPage + 1
                var reachedEnd = false
                var fetchedEnoughForNextBatch = false
                
                while (page <= currentPage + 20) {
                    val newBatch: List<TMDBSearchResult>
                    var totalPages = 1

                    val config = _uiState.value.sortConfig
                    val hasFilters = config.selectedGenres.isNotEmpty() || config.selectedKeywords.isNotEmpty() || config.selectedDecades.isNotEmpty() || config.sortType != "popularity"
                    
                    if (query.isEmpty() && hasFilters && category != "person") {
                        val options = buildTmdbOptions(config, category)

                        when (category) {
                            "movie" -> {
                                val response = tmdbService.discoverMovies(page = page, options = options)
                                newBatch = response.results.map { it.toMovieResultInternal() }
                                totalPages = response.totalPages ?: 1
                            }
                            "tv" -> {
                                val response = tmdbService.discoverTV(page = page, options = options)
                                newBatch = response.results.map { it.toTvResultInternal() }
                                totalPages = response.totalPages ?: 1
                            }
                            else -> {
                                newBatch = emptyList()
                                totalPages = 0
                            }
                        }
                    } else {
                        when (category) {
                            "movie" -> {
                                val response = tmdbService.searchMovie(query, page = page)
                                newBatch = response.results.map { it.toMovieResultInternal() }
                                totalPages = response.totalPages ?: 1
                            }
                            "tv" -> {
                                val response = tmdbService.searchTV(query, page = page)
                                newBatch = response.results.map { it.toTvResultInternal() }
                                totalPages = response.totalPages ?: 1
                            }
                            "person" -> {
                                val response = tmdbService.searchPeople(query, page = page)
                                newBatch = response.results.map { 
                                    TMDBSearchResult.PersonResult(it.id, it.name, it.profilePath, it.knownForDepartment)
                                }
                                totalPages = response.totalPages ?: 1
                            }
                            else -> {
                                val response = tmdbService.searchMulti(query, page = page)
                                newBatch = response.results
                                totalPages = response.totalPages ?: 1
                            }
                        }
                    }

                    if (newBatch.isEmpty()) {
                        reachedEnd = true
                        break
                    }

                    rawResults = (rawResults + newBatch).distinctBy { result ->
                        when (result) {
                            is TMDBSearchResult.MovieResult -> "movie_${result.id}"
                            is TMDBSearchResult.TvResult -> "tv_${result.id}"
                            is TMDBSearchResult.PersonResult -> "person_${result.id}"
                            else -> "other_${result.id}"
                        }
                    }
                    applyStateFilters()
                    currentPage = page
                    
                    if (page >= totalPages) {
                        reachedEnd = true
                        break
                    }

                    val filteredFromThisBatch = newBatch.applyFilter(_uiState.value.sortConfig)
                    if (filteredFromThisBatch.isNotEmpty() || _uiState.value.sortConfig.selectedGenres.isEmpty() || category == "person") {
                        fetchedEnoughForNextBatch = true
                        break
                    }
                    page++
                }
                
                _uiState.update { it.copy(isEndReached = reachedEnd) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(isEndReached = true) }
            } finally {
                _uiState.update { it.copy(isNextPageLoading = false) }
            }
        }
    }

    fun toggleFavorite(movie: Movie) {
        if (_uiState.value.togglingIds.contains(movie.id)) return
        
        val title = movie.title ?: movie.name ?: ""
        viewModelScope.launch {
            _uiState.update { it.copy(togglingIds = it.togglingIds.add(movie.id)) }
            try {
                // 1. Fetch current database state to have an accurate baseline
                val local = repository.getMovie(movie.id, movie.mediaType)
                val current = local ?: movie
                val previousState = current.copy()
                
                // 2. IDEMPOTENCY CHECK: If already watched, do nothing
                if (current.watched) {
                    return@launch
                }

                // 3. Perform the cycle
                android.util.Log.d("SearchViewModel", "toggleFavorite: cycling status for $title (ID: ${movie.id})")
                cycleMovieStatusUseCase(current)
                
                // 3. Re-fetch updated state to determine the correct label
                val updated = repository.getMovie(movie.id, movie.mediaType)
                android.util.Log.d("SearchViewModel", "toggleFavorite: updated state for $title: watched=${updated?.watched}, fav=${updated?.favorite}, rem=${updated?.reminder}")
                
                val actionMsgRes = when {
                    updated == null -> R.string.msg_action_removed
                    updated.watched -> R.string.msg_action_watched
                    updated.favorite -> R.string.msg_action_favorite
                    updated.reminder -> R.string.msg_action_reminder
                    else -> R.string.msg_action_updated
                }

                actionFeedbackManager.emit(UiText.StringResource(actionMsgRes, title)) {
                    android.util.Log.d("SearchViewModel", "toggleFavorite: UNDO clicked for $title")
                    repository.saveMovie(previousState)
                }
            } catch (e: Exception) {
                android.util.Log.e("SearchViewModel", "Error toggling favorite for $title", e)
            } finally {
                _uiState.update { it.copy(togglingIds = it.togglingIds.remove(movie.id)) }
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

    fun deleteRecentSearch(query: String) {
        viewModelScope.launch {
            try {
                repository.deleteSearchQuery(query)
            } catch (e: Exception) {
                android.util.Log.e("SearchViewModel", "Error deleting recent search", e)
            }
        }
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            try {
                repository.clearRecentSearches()
            } catch (e: Exception) {
                android.util.Log.e("SearchViewModel", "Error clearing recent searches", e)
            }
        }
    }

    fun updateRating(movie: Movie, rating: Double) {
        viewModelScope.launch {
            try {
                val local = repository.getMovie(movie.id, movie.mediaType)
                val current = local ?: movie
                repository.saveMovie(current.copy(personalRating = rating))
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

    fun toggleItemInFolder(folder: FolderEntity, movie: Movie) {
        viewModelScope.launch {
            try {
                val compositeId = "${movie.mediaType}_${movie.id}"
                val newItemIds = if (folder.itemIds.contains(compositeId)) {
                    folder.itemIds - compositeId
                } else {
                    folder.itemIds + compositeId
                }
                repository.saveFolder(folder.copy(itemIds = newItemIds, updatedAt = java.time.Instant.now().toString()))
            } catch (e: Exception) {
                actionFeedbackManager.emit(UiText.StringResource(R.string.msg_error_updating_folder))
            }
        }
    }

    private fun Movie.toMovieResultInternal() = TMDBSearchResult.MovieResult(
        id = id,
        title = title,
        posterPath = posterPath,
        backdropPath = backdropPath,
        voteAverage = voteAverage,
        releaseDate = releaseDate,
        genreIds = genreIds?.map { it.toLong() } ?: emptyList(),
        overview = overview
    )

    private fun Movie.toTvResultInternal() = TMDBSearchResult.TvResult(
        id = id,
        name = name,
        posterPath = posterPath,
        backdropPath = backdropPath,
        voteAverage = voteAverage,
        firstAirDate = firstAirDate,
        genreIds = genreIds?.map { it.toLong() } ?: emptyList(),
        overview = overview
    )

    fun updatePreferences(prefs: com.cinetrack.data.models.UserPreferences) {
        viewModelScope.launch {
            try {
                preferenceRepository.updateAll(prefs)
            } catch (e: Exception) {
                android.util.Log.e("SearchViewModel", "Error updating preferences", e)
            }
        }
    }

    fun toggleSuggestionsExpanded() {
        val current = _uiState.value.preferences.isSearchSuggestionsExpanded
        viewModelScope.launch {
            try {
                preferenceRepository.updateIsSearchSuggestionsExpanded(!current)
            } catch (e: Exception) {
                android.util.Log.e("SearchViewModel", "Error updating suggestions expanded preference", e)
            }
        }
    }
}


