package com.cinetrack.domain

import com.cinetrack.data.Movie
import com.cinetrack.data.models.SortConfig
import com.cinetrack.data.models.UserPreferences
import com.cinetrack.data.local.entities.FolderEntity
import com.cinetrack.ui.viewmodel.HomeUiState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Dispatchers
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap

class GetHomeUiStateUseCase @Inject constructor() {
    operator fun invoke(
        moviesFlow: Flow<List<Movie>>,
        foldersFlow: Flow<List<FolderEntity>>,
        preferencesFlow: Flow<UserPreferences>,
        searchQueryFlow: Flow<String>,
        activeTabFlow: Flow<String>
    ): Flow<HomeUiState> {
        @OptIn(kotlinx.coroutines.FlowPreview::class)
        val baseMoviesFlow = moviesFlow.map { movies -> 
            movies.filter { (it.favorite || it.reminder) && !it.watched } 
        }.distinctUntilChanged()
        
        @OptIn(kotlinx.coroutines.FlowPreview::class)
        val debouncedSearch = searchQueryFlow
            .debounce(300)
            .distinctUntilChanged()

        return combine(
            baseMoviesFlow,
            foldersFlow,
            preferencesFlow,
            debouncedSearch,
            activeTabFlow
        ) { toWatchMovies, folders, prefs, query, tab ->
            
            val movieCount = toWatchMovies.count { it.mediaType != "tv" }
            val tvCount = toWatchMovies.count { it.mediaType == "tv" }

            val filtered = toWatchMovies.asSequence()
                .filter { if (tab == "movie") it.mediaType != "tv" else it.mediaType == "tv" }
                .filter { query.isEmpty() || it.title?.contains(query, ignoreCase = true) == true || it.name?.contains(query, ignoreCase = true) == true }
                .filter { prefs.homeSort.selectedGenres.isEmpty() || it.genreIds?.any { g -> g in prefs.homeSort.selectedGenres } == true }
                .filter { movie ->
                    prefs.homeSort.selectedDecades.isEmpty() || 
                    prefs.homeSort.selectedDecades.any { decade ->
                        val prefix = decade.take(3)
                        (movie.releaseDate?.startsWith(prefix) == true) || (movie.firstAirDate?.startsWith(prefix) == true)
                    }
                }
                .filter { prefs.homeSort.selectedProviders.isEmpty() || it.streamingProviderIds?.any { p -> p in prefs.homeSort.selectedProviders } == true }
                .toList()

            val sorted: List<Movie> = sortMovies(filtered, prefs.homeSort)
            
            val partitionResult: Pair<List<Movie>, List<Movie>> = sorted.partition { !it.isReleased }
            val unreleased = partitionResult.first
            val released = partitionResult.second

            val today = LocalDate.now().toString()
            val notificationCount = toWatchMovies.count { movie ->
                val isNewEpisode = (movie.newEpisodesFound ?: 0) > 0
                val isReleasedToday = (movie.newEpisodesFound ?: 0) == 0 && 
                                        movie.reminder == true && 
                                        movie.migratedAt == today
                isNewEpisode || isReleasedToday
            }

            val movieFolderColors = buildMap<String, MutableList<String>> {
                folders.forEach { folder ->
                    val color = folder.color ?: "#FFFFFF"
                    folder.itemIds.forEach { itemId ->
                        getOrPut(itemId) { mutableListOf() }.add(color)
                    }
                }
            }.mapValues { it.value.toImmutableList() }.toImmutableMap()

            HomeUiState(
                movies = sorted.toImmutableList(),
                releasedMovies = released.toImmutableList(),
                unreleasedMovies = unreleased.toImmutableList(),
                movieCount = movieCount,
                tvCount = tvCount,
                isLoading = false,
                searchQuery = query,
                activeTab = tab,
                notificationCount = notificationCount,
                movieFolderColors = movieFolderColors,
                folders = folders.toImmutableList(),
                sortConfig = prefs.homeSort,
                preferences = prefs
            )
        }.flowOn(Dispatchers.Default)
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
