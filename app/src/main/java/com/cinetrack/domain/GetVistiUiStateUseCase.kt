package com.cinetrack.domain

import com.cinetrack.data.model.Movie
import com.cinetrack.data.model.SortConfig
import com.cinetrack.data.model.UserPreferences
import com.cinetrack.data.local.entities.FolderEntity
import com.cinetrack.ui.viewmodel.VistiUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap

class GetVistiUiStateUseCase @Inject constructor() {
    operator fun invoke(
        moviesFlow: Flow<List<Movie>>,
        foldersFlow: Flow<List<FolderEntity>>,
        preferencesFlow: Flow<UserPreferences>,
        searchQueryFlow: Flow<String>,
        activeTabFlow: Flow<String>
    ): Flow<VistiUiState> {
        return combine(
            moviesFlow,
            foldersFlow,
            preferencesFlow,
            searchQueryFlow,
            activeTabFlow
        ) { movies, folders, prefs, query, tab ->
            val watchedMovies = movies.filter { it.watched }.distinctBy { "${it.id}_${it.mediaType}" }
            val movieCount = watchedMovies.count { it.mediaType != "tv" }
            val tvCount = watchedMovies.count { it.mediaType == "tv" }
            
            val filtered = watchedMovies.filter { movie ->
                val matchesTab = if (tab == "movie") movie.mediaType != "tv" else movie.mediaType == "tv"
                val matchesSearch = query.isEmpty() || 
                    (movie.title?.contains(query, ignoreCase = true) ?: false) ||
                    (movie.name?.contains(query, ignoreCase = true) ?: false)
                
                val matchesGenre = prefs.vistiSort.selectedGenres.isEmpty() || 
                    (movie.genreIds?.any { it in prefs.vistiSort.selectedGenres } ?: false)
                
                val matchesDecade = prefs.vistiSort.selectedDecades.isEmpty() || 
                    prefs.vistiSort.selectedDecades.any { decade ->
                        val prefix = decade.take(3)
                        (movie.releaseDate?.startsWith(prefix) ?: false) ||
                        (movie.firstAirDate?.startsWith(prefix) ?: false)
                    }

                val matchesProvider = prefs.vistiSort.selectedProviders.isEmpty() || 
                    (movie.streamingProviderIds?.any { it in prefs.vistiSort.selectedProviders } ?: false)
                
                matchesTab && matchesSearch && matchesGenre && matchesDecade && matchesProvider
            }

            val sorted: List<Movie> = sortMovies(filtered, prefs.vistiSort)

            val movieFolderColors = mutableMapOf<String, MutableList<String>>()
            folders.forEach { folder ->
                val color = folder.color ?: "#FFFFFF"
                folder.itemIds.forEach { itemId ->
                    movieFolderColors.getOrPut(itemId) { mutableListOf() }.add(color)
                }
            }
            val immutableMovieFolderColors = movieFolderColors.mapValues { it.value.toImmutableList() }.toImmutableMap()

            VistiUiState(
                movies = sorted.toImmutableList(),
                movieCount = movieCount,
                tvCount = tvCount,
                isLoading = false,
                searchQuery = query,
                activeTab = tab,
                movieFolderColors = immutableMovieFolderColors,
                folders = folders.toImmutableList(),
                sortConfig = prefs.vistiSort,
                preferences = prefs
            )
        }.flowOn(Dispatchers.Default)
    }

    private fun sortMovies(movies: List<Movie>, sort: SortConfig): List<Movie> {
        val isDesc = sort.sortDirection == "desc"
        return when (sort.sortType) {
            "watched_at" -> {
                if (isDesc) {
                    movies.sortedWith(compareByDescending<Movie> { it.watchedAt }.thenBy { it.title ?: it.name ?: "" }.thenBy { it.id })
                } else {
                    movies.sortedWith(compareBy<Movie> { it.watchedAt }.thenBy { it.title ?: it.name ?: "" }.thenBy { it.id })
                }
            }
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
