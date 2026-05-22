package com.cinetrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinetrack.data.Movie
import com.cinetrack.data.repository.MovieRepository
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
import com.cinetrack.data.local.entities.FolderEntity

data class SearchUiState(
    val query: String = "",
    val category: String = "movie",
    val results: List<TMDBSearchResult> = emptyList(),
    val trendingMovies: List<TMDBSearchResult> = emptyList(),
    val trendingTv: List<TMDBSearchResult> = emptyList(),
    val trendingPeople: List<TMDBSearchResult> = emptyList(),
    val isLoading: Boolean = false,
    val isNextPageLoading: Boolean = false,
    val isEndReached: Boolean = false,
    val favorites: List<Movie> = emptyList(),
    val folders: List<FolderEntity> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val sortConfig: SortConfig = SortConfig(),
    val movieFolderColors: Map<String, List<String>> = emptyMap(),
    val preferences: com.cinetrack.data.models.UserPreferences = com.cinetrack.data.models.UserPreferences(),
    val togglingIds: Set<Long> = emptySet(),
    val errorMessage: String? = null
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: MovieRepository,
    private val preferenceRepository: com.cinetrack.data.repository.PreferenceRepository,
    private val tmdbService: TMDBService,
    private val actionFeedbackManager: ActionFeedbackManager,
    @Named("tmdb_api_key") private val apiKey: String
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _category = MutableStateFlow("movie")
    private val _results = MutableStateFlow<List<TMDBSearchResult>>(emptyList())
    private val _trendingMovies = MutableStateFlow<List<TMDBSearchResult>>(emptyList())
    private val _trendingTv = MutableStateFlow<List<TMDBSearchResult>>(emptyList())
    private val _trendingPeople = MutableStateFlow<List<TMDBSearchResult>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _isNextPageLoading = MutableStateFlow(false)
    private val _isEndReached = MutableStateFlow(false)
    private val _sortConfig = MutableStateFlow(SortConfig())
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _togglingIds = MutableStateFlow<Set<Long>>(emptySet())
    private var currentPage = 1
    
    fun emitMessage(message: String) {
        actionFeedbackManager.emit(message)
    }
    
    private var searchJob: Job? = null
    
    init {
        // Initial fetch for trending or discovery
        performSearch()

        // Reactive search handling with debouncing for query
        viewModelScope.launch {
            _query
                .debounce { q -> if (q.isEmpty()) 0L else 500L }
                .distinctUntilChanged()
                .collect { 
                    performSearch()
                }
        }

        // Category and Sort changes trigger search immediately
        viewModelScope.launch {
            combine(_category, _sortConfig) { c, s -> c to s }
                .drop(1) // Skip initial values as performSearch() is called above
                .distinctUntilChanged()
                .collect {
                    performSearch()
                }
        }
    }

    val uiState: StateFlow<SearchUiState> = combine(
        combine(
            _query,
            _category,
            _results,
            _trendingMovies,
            _trendingTv
        ) { query, category, results, trendingMovies, trendingTv ->
            Triple(query, category, Triple(results, trendingMovies, trendingTv))
        },
        combine(
            _trendingPeople,
            _isLoading,
            _isNextPageLoading,
            _isEndReached,
            _sortConfig
        ) { trendingPeople, loading, nextLoading, endReached, sort ->
            Triple(trendingPeople, Triple(loading, nextLoading, endReached), sort)
        },
        combine(
            _errorMessage,
            repository.getLocalMoviesFlow(),
            repository.getFoldersFlow(),
            repository.getRecentSearches(),
            combine(preferenceRepository.userPreferencesFlow, _togglingIds) { prefs, ids -> prefs to ids }
        ) { error, local, folders, recent, prefsAndIds ->
            val (prefs, togglingIds) = prefsAndIds
            Triple(error, Pair(local, folders), Pair(recent, Pair(prefs, togglingIds)))
        }
    ) { groupA, groupB, groupC ->
        android.util.Log.d("SearchViewModel", "uiState combine triggered: localMovies size = ${groupC.second.first.size}")
        val query = groupA.first
        val category = groupA.second
        val (resultsList, trendingMoviesList, trendingTvList) = groupA.third
        
        val trendingPeopleList = groupB.first
        val (isLoading, isNextPageLoading, isEndReached) = groupB.second
        val sortConfig = groupB.third
        
        val errorMessage = groupC.first
        val (localMovies, folders) = groupC.second
        val recentSearches = groupC.third.first
        val (prefs, togglingIds) = groupC.third.second
        
        // DEBUG LOG
        localMovies.find { it.id == 1007757L }?.let {
            android.util.Log.d("SearchViewModel", "DEBUG: Movie 1007757 in favorites: watched=${it.watched}, favorite=${it.favorite}, reminder=${it.reminder}")
        }
        
        val results = resultsList.distinctBy { "${it.id}_${it.mediaType}" }
        val trendingMovies = trendingMoviesList.distinctBy { "${it.id}_${it.mediaType}" }
        val trendingTv = trendingTvList.distinctBy { "${it.id}_${it.mediaType}" }
        val trendingPeople = trendingPeopleList.distinctBy { "${it.id}_${it.mediaType}" }

        // Apply local filtering based on sortConfig
        val filteredResults = results.applyFilter(sortConfig).let { list ->
            val isDesc = sortConfig.sortDirection == "desc"
            
            val sortedList = when (sortConfig.sortType) {
                "vote_average" -> {
                    if (isDesc) list.sortedByDescending { it.voteAverage }
                    else list.sortedBy { it.voteAverage }
                }
                "personal_rating" -> {
                    if (isDesc) {
                        list.sortedByDescending { result ->
                            localMovies.find { it.id == result.id && it.mediaType == result.mediaType }?.personalRating ?: 0.0
                        }
                    } else {
                        list.sortedBy { result ->
                            localMovies.find { it.id == result.id && it.mediaType == result.mediaType }?.personalRating ?: 0.0
                        }
                    }
                }
                "release_date" -> {
                    if (isDesc) list.sortedByDescending { it.releaseDate }
                    else list.sortedBy { it.releaseDate }
                }
                "title" -> {
                    if (isDesc) list.sortedByDescending { it.title }
                    else list.sortedBy { it.title }
                }
                else -> list
            }
            sortedList
        }

        val movieFolderColors = mutableMapOf<String, List<String>>()
        folders.forEach { folder ->
            val color = folder.color ?: "#FFFFFF"
            folder.itemIds.forEach { itemId ->
                val colors = movieFolderColors.getOrDefault(itemId, emptyList())
                movieFolderColors[itemId] = colors + color
            }
        }

        SearchUiState(
            query = query,
            category = category,
            results = filteredResults,
            trendingMovies = trendingMovies,
            trendingTv = trendingTv,
            trendingPeople = trendingPeople,
            isLoading = isLoading,
            isNextPageLoading = isNextPageLoading,
            isEndReached = isEndReached,
            favorites = localMovies,
            folders = folders,
            recentSearches = recentSearches,
            sortConfig = sortConfig,
            movieFolderColors = movieFolderColors,
            preferences = prefs,
            togglingIds = togglingIds,
            errorMessage = errorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SearchUiState()
    )

    fun onQueryChanged(newQuery: String) {
        if (_query.value == newQuery) return
        _query.value = newQuery
    }

    fun onCategoryChanged(newCategory: String) {
        if (_category.value == newCategory) return
        _category.value = newCategory
        // Reset selected genres on category change to avoid cross-contamination of movie/tv genre IDs
        _sortConfig.value = _sortConfig.value.copy(selectedGenres = emptyList())
    }

    fun updateSortConfig(config: SortConfig) {
        _sortConfig.value = config
    }

    fun retry() {
        performSearch()
    }

    private fun performSearch() {
        searchJob?.cancel()
        val query = _query.value
        val category = _category.value
        _errorMessage.value = null

        if (query.isEmpty()) {
            if (_sortConfig.value.selectedGenres.isNotEmpty() && category != "person") {
                fetchDiscovery()
            } else {
                _results.value = emptyList()
                fetchTrending()
            }
            return
        }

        _results.value = emptyList()

        if (query.length <= 2) {
            _isLoading.value = false
            _isEndReached.value = false
            currentPage = 1
            return
        }
        _isEndReached.value = false
        currentPage = 1

        searchJob = viewModelScope.launch {
            _isLoading.value = true
            try {
                var page = 1
                var accumulatedResults = emptyList<TMDBSearchResult>()
                var reachedEnd = false
                
                while (page <= 15) {
                    val newBatch: List<TMDBSearchResult>
                    var totalPages = 1

                    when (category) {
                        "movie" -> {
                            val response = tmdbService.searchMovie(query, apiKey, page = page)
                            newBatch = response.results.map { it.toMovieResultInternal() }
                            totalPages = response.totalPages ?: 1
                        }
                        "tv" -> {
                            val response = tmdbService.searchTV(query, apiKey, page = page)
                            newBatch = response.results.map { it.toTvResultInternal() }
                            totalPages = response.totalPages ?: 1
                        }
                        "person" -> {
                            val response = tmdbService.searchPeople(query, apiKey, page = page)
                            newBatch = response.results.map { 
                                TMDBSearchResult.PersonResult(it.id, it.name, it.profilePath, it.knownForDepartment)
                            }
                            totalPages = response.totalPages ?: 1
                        }
                        else -> {
                            val response = tmdbService.searchMulti(query, apiKey, page = page)
                            newBatch = response.results
                            totalPages = response.totalPages ?: 1
                        }
                    }

                    if (newBatch.isEmpty()) {
                        reachedEnd = true
                        break
                    }

                    accumulatedResults = accumulatedResults + newBatch
                    _results.value = accumulatedResults
                    currentPage = page
                    
                    if (page >= totalPages) {
                        reachedEnd = true
                        break
                    }

                    val filteredCount = accumulatedResults.applyFilter(_sortConfig.value).size
                    if (_sortConfig.value.selectedGenres.isEmpty() || category == "person" || filteredCount >= 40) {
                        break
                    }
                    page++
                }
                
                _isEndReached.value = reachedEnd

                if (accumulatedResults.isEmpty() && query.length >= 3) {
                    // FALLBACK: If no results found, try a fuzzy match by using a prefix of the longest word
                    val fallbackQuery = com.cinetrack.ui.utils.FuzzySearch.buildFallbackQuery(query)
                    if (fallbackQuery != null && fallbackQuery != query) {
                        var fallbackPage = 1
                        while (fallbackPage <= 2) {
                            val fallbackBatch: List<TMDBSearchResult> = when (category) {
                                "movie" -> tmdbService.searchMovie(fallbackQuery, apiKey, page = fallbackPage).results.map { it.toMovieResultInternal() }
                                "tv" -> tmdbService.searchTV(fallbackQuery, apiKey, page = fallbackPage).results.map { it.toTvResultInternal() }
                                "person" -> tmdbService.searchPeople(fallbackQuery, apiKey, page = fallbackPage).results.map { TMDBSearchResult.PersonResult(it.id, it.name, it.profilePath, it.knownForDepartment) }
                                else -> tmdbService.searchMulti(fallbackQuery, apiKey, page = fallbackPage).results
                            }
                            
                            if (fallbackBatch.isEmpty()) break
                            
                            // Score and filter fallback results
                            val scoredFallback = fallbackBatch.filter { res ->
                                com.cinetrack.ui.utils.FuzzySearch.score(query, res.displayTitle) > 0.45
                            }.sortedByDescending { res ->
                                com.cinetrack.ui.utils.FuzzySearch.score(query, res.displayTitle)
                            }
                            
                            if (scoredFallback.isNotEmpty()) {
                                accumulatedResults = (accumulatedResults + scoredFallback).distinctBy { "${it.id}_${it.mediaType}" }
                                _results.value = accumulatedResults
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
                _isEndReached.value = true
                _errorMessage.value = ErrorMapper.map(e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun fetchDiscovery() {
        searchJob = viewModelScope.launch {
            _isLoading.value = true
            _isEndReached.value = false
            currentPage = 1
            try {
                val category = _category.value
                val genreId = _sortConfig.value.selectedGenres.firstOrNull()
                
                if (category == "person") {
                    _results.value = emptyList()
                    fetchTrending()
                    return@launch
                }

                // If we have a genre, we fetch by genre. If not, we fetch trending and then apply local decade filter.
                // Note: Full decade-based TMDB discovery would require a repository update.
                val results = if (genreId != null) {
                    coroutineScope {
                        (1..5).map { pageNum ->
                            async {
                                try {
                                    when (category) {
                                        "movie" -> repository.getMoviesByGenre(genreId, pageNum).map { it.toMovieResultInternal() }
                                        "tv" -> repository.getTVShowsByGenre(genreId, pageNum).map { it.toTvResultInternal() }
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
                    // Fallback to trending and filter locally by decade
                    when (category) {
                        "movie" -> repository.getTrendingMovies(1).map { it.toMovieResultInternal() }
                        "tv" -> repository.getTrendingTV(1).map { it.toTvResultInternal() }
                        else -> emptyList()
                    }
                }

                _results.value = results
                currentPage = 5
                _isEndReached.value = results.size < 100
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _results.value = emptyList()
                _errorMessage.value = ErrorMapper.map(e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun fetchTrending() {
        searchJob = viewModelScope.launch {
            _isLoading.value = true
            _isEndReached.value = false
            currentPage = 1
            try {
                val movies = repository.getTrendingMovies(1).take(6).map { it.toMovieResultInternal() }
                val tv = repository.getTrendingTV(1).take(6).map { it.toTvResultInternal() }
                val people = repository.getPopularPeople(1).take(6).map { 
                    TMDBSearchResult.PersonResult(it.id, it.name, it.profilePath, it.knownForDepartment)
                }
                
                _trendingMovies.value = movies
                _trendingTv.value = tv
                _trendingPeople.value = people
                _results.value = emptyList()
                _isEndReached.value = true
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                android.util.Log.e("SearchViewModel", "fetchTrending failed!", e)
                _errorMessage.value = ErrorMapper.map(e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadNextPage() {
        if (_isNextPageLoading.value || _isEndReached.value || _isLoading.value) return
        val query = _query.value
        val category = _category.value

        viewModelScope.launch {
            _isNextPageLoading.value = true
            try {
                var page = currentPage + 1
                var reachedEnd = false
                var fetchedEnoughForNextBatch = false
                
                while (page <= currentPage + 20) {
                    val newBatch: List<TMDBSearchResult>
                    var totalPages = 1

                    if (query.isEmpty() && _sortConfig.value.selectedGenres.isNotEmpty()) {
                        val genreId = _sortConfig.value.selectedGenres.first()
                        when (category) {
                            "movie" -> {
                                val response = tmdbService.getMoviesByGenre(genreId, apiKey, page = page)
                                newBatch = response.results.map { it.toMovieResultInternal() }
                                totalPages = response.totalPages ?: 1
                            }
                            "tv" -> {
                                val response = tmdbService.getTVShowsByGenre(genreId, apiKey, page = page)
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
                                val response = tmdbService.searchMovie(query, apiKey, page = page)
                                newBatch = response.results.map { it.toMovieResultInternal() }
                                totalPages = response.totalPages ?: 1
                            }
                            "tv" -> {
                                val response = tmdbService.searchTV(query, apiKey, page = page)
                                newBatch = response.results.map { it.toTvResultInternal() }
                                totalPages = response.totalPages ?: 1
                            }
                            "person" -> {
                                val response = tmdbService.searchPeople(query, apiKey, page = page)
                                newBatch = response.results.map { 
                                    TMDBSearchResult.PersonResult(it.id, it.name, it.profilePath, it.knownForDepartment)
                                }
                                totalPages = response.totalPages ?: 1
                            }
                            else -> {
                                val response = tmdbService.searchMulti(query, apiKey, page = page)
                                newBatch = response.results
                                totalPages = response.totalPages ?: 1
                            }
                        }
                    }

                    if (newBatch.isEmpty()) {
                        reachedEnd = true
                        break
                    }

                    _results.value = (_results.value + newBatch).distinctBy { result ->
                        when (result) {
                            is TMDBSearchResult.MovieResult -> "movie_${result.id}"
                            is TMDBSearchResult.TvResult -> "tv_${result.id}"
                            is TMDBSearchResult.PersonResult -> "person_${result.id}"
                        }
                    }
                    currentPage = page
                    
                    if (page >= totalPages) {
                        reachedEnd = true
                        break
                    }

                    val filteredFromThisBatch = newBatch.applyFilter(_sortConfig.value)
                    if (filteredFromThisBatch.isNotEmpty() || _sortConfig.value.selectedGenres.isEmpty() || category == "person") {
                        fetchedEnoughForNextBatch = true
                        break
                    }
                    page++
                }
                
                _isEndReached.value = reachedEnd
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _isEndReached.value = true
            } finally {
                _isNextPageLoading.value = false
            }
        }
    }

    fun toggleFavorite(movie: Movie) {
        if (_togglingIds.value.contains(movie.id)) return
        
        val title = movie.title ?: movie.name ?: ""
        viewModelScope.launch {
            _togglingIds.update { it + movie.id }
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
                repository.cycleMovieStatus(current)
                
                // 3. Re-fetch updated state to determine the correct label
                val updated = repository.getMovie(movie.id, movie.mediaType)
                android.util.Log.d("SearchViewModel", "toggleFavorite: updated state for $title: watched=${updated?.watched}, fav=${updated?.favorite}, rem=${updated?.reminder}")
                
                val actionLabel = when {
                    updated == null -> "rimosso"
                    updated.watched -> "segnato come visto"
                    updated.favorite -> "aggiunto a Da Vedere"
                    updated.reminder -> "promemoria impostato"
                    !updated.favorite && !updated.watched && !updated.reminder -> "rimosso"
                    else -> "aggiornato"
                }
                
                actionFeedbackManager.emit("\"$title\" $actionLabel") {
                    android.util.Log.d("SearchViewModel", "toggleFavorite: UNDO clicked for $title")
                    repository.saveMovie(previousState)
                }
            } catch (e: Exception) {
                android.util.Log.e("SearchViewModel", "Error toggling favorite for $title", e)
            } finally {
                _togglingIds.update { it - movie.id }
            }
        }
    }

    fun deleteMovie(movie: Movie) {
        viewModelScope.launch {
            repository.deleteMovie(movie)
            actionFeedbackManager.emit("\"${movie.title ?: movie.name}\" rimosso") {
                repository.saveMovie(movie)
            }
        }
    }

    fun deleteRecentSearch(query: String) {
        viewModelScope.launch {
            repository.deleteSearchQuery(query)
        }
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            repository.clearRecentSearches()
        }
    }

    fun updateRating(movie: Movie, rating: Double) {
        viewModelScope.launch {
            repository.saveMovie(movie.copy(personalRating = rating))
        }
    }

    fun updateNote(movie: Movie, note: String) {
        viewModelScope.launch {
            repository.saveMovie(movie.copy(personalNote = note))
        }
    }

    fun toggleItemInFolder(folder: FolderEntity, movie: Movie) {
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

    private fun List<TMDBSearchResult>.applyFilter(config: SortConfig): List<TMDBSearchResult> {
        return this.filter { result ->
            if (result is TMDBSearchResult.PersonResult) return@filter true

            // 1. Genre Filter
            val genreMatch = if (config.selectedGenres.isEmpty()) true else {
                val genreIds = when (result) {
                    is TMDBSearchResult.MovieResult -> result.genreIds
                    is TMDBSearchResult.TvResult -> result.genreIds
                    else -> emptyList()
                }
                config.selectedGenres.any { it.toLong() in genreIds }
            }

            // 2. Decade Filter
            val decadeMatch = if (config.selectedDecades.isEmpty()) true else {
                val date = when (result) {
                    is TMDBSearchResult.MovieResult -> result.releaseDate
                    is TMDBSearchResult.TvResult -> result.firstAirDate
                    else -> null
                }
                val year = date?.split("-")?.firstOrNull()?.toIntOrNull()
                if (year != null) {
                    config.selectedDecades.any { decadeStr ->
                        val start = decadeStr.removeSuffix("s").toIntOrNull()
                        start != null && year in start..(start + 9)
                    }
                } else false
            }

            genreMatch && decadeMatch
        }
    }
}

private val TMDBSearchResult.voteAverage: Double
    get() = when (this) {
        is TMDBSearchResult.MovieResult -> voteAverage ?: 0.0
        is TMDBSearchResult.TvResult -> voteAverage ?: 0.0
        else -> 0.0
    }

private val TMDBSearchResult.releaseDate: String
    get() = when (this) {
        is TMDBSearchResult.MovieResult -> releaseDate ?: ""
        is TMDBSearchResult.TvResult -> firstAirDate ?: ""
        else -> ""
    }

private val TMDBSearchResult.title: String
    get() = when (this) {
        is TMDBSearchResult.MovieResult -> title ?: ""
        is TMDBSearchResult.TvResult -> name ?: ""
        is TMDBSearchResult.PersonResult -> name
    }

private val TMDBSearchResult.mediaType: String
    get() = when (this) {
        is TMDBSearchResult.MovieResult -> "movie"
        is TMDBSearchResult.TvResult -> "tv"
        is TMDBSearchResult.PersonResult -> "person"
    }
