package com.cinetrack.domain

import com.cinetrack.data.Movie
import com.cinetrack.data.models.SortConfig
import com.cinetrack.data.models.UserPreferences
import com.cinetrack.data.local.entities.FolderEntity
import com.cinetrack.ui.viewmodel.HomeUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
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
        return combine(
            moviesFlow,
            foldersFlow,
            preferencesFlow,
            searchQueryFlow,
            activeTabFlow
        ) { movies, folders, prefs, query, tab ->
            val toWatchMovies = movies.filter { (it.favorite || it.reminder) && !it.watched }
            val movieCount = toWatchMovies.count { it.mediaType != "tv" }
            val tvCount = toWatchMovies.count { it.mediaType == "tv" }

            val filtered = toWatchMovies.filter { movie ->
                val matchesTab = if (tab == "movie") movie.mediaType != "tv" else movie.mediaType == "tv"
                val matchesSearch = query.isEmpty() || 
                    (movie.title?.contains(query, ignoreCase = true) ?: false) ||
                    (movie.name?.contains(query, ignoreCase = true) ?: false)
                
                val matchesGenre = prefs.homeSort.selectedGenres.isEmpty() || 
                    (movie.genreIds?.any { it in prefs.homeSort.selectedGenres } ?: false)
                
                val matchesDecade = prefs.homeSort.selectedDecades.isEmpty() || 
                    prefs.homeSort.selectedDecades.any { decade ->
                        val prefix = decade.take(3)
                        (movie.releaseDate?.startsWith(prefix) ?: false) ||
                        (movie.firstAirDate?.startsWith(prefix) ?: false)
                    }

                val matchesProvider = prefs.homeSort.selectedProviders.isEmpty() || 
                    (movie.streamingProviderIds?.any { it in prefs.homeSort.selectedProviders } ?: false)
                
                matchesTab && matchesSearch && matchesGenre && matchesDecade && matchesProvider
            }

            val sorted: List<Movie> = sortMovies(filtered, prefs.homeSort)
            
            val today = LocalDate.now().toString()
            val partitionResult: Pair<List<Movie>, List<Movie>> = sorted.partition { movie: Movie ->
                val date = movie.releaseDate ?: movie.firstAirDate
                date != null && date > today && date.isNotEmpty()
            }
            val unreleased = partitionResult.first
            val released = partitionResult.second

            val notificationCount = movies.count { movie ->
                val isNewEpisode = (movie.newEpisodesFound ?: 0) > 0
                val isReleasedToday = (movie.newEpisodesFound ?: 0) == 0 && 
                                        movie.reminder == true && 
                                        movie.migratedAt == today
                isNewEpisode || isReleasedToday
            }

            val movieFolderColors = mutableMapOf<String, MutableList<String>>()
            folders.forEach { folder ->
                val color = folder.color ?: "#FFFFFF"
                folder.itemIds.forEach { itemId ->
                    movieFolderColors.getOrPut(itemId) { mutableListOf() }.add(color)
                }
            }
            val immutableMovieFolderColors = movieFolderColors.mapValues { it.value.toImmutableList() }.toImmutableMap()

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
                movieFolderColors = immutableMovieFolderColors,
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
                    movies.sortedByDescending { it.releaseDate ?: it.firstAirDate ?: "" }
                } else {
                    movies.sortedBy { it.releaseDate ?: it.firstAirDate ?: "" }
                }
            }
            "title" -> {
                if (isDesc) {
                    movies.sortedByDescending { it.title ?: it.name ?: "" }
                } else {
                    movies.sortedBy { it.title ?: it.name ?: "" }
                }
            }
            "added_at", "created_at" -> {
                if (isDesc) {
                    movies.sortedByDescending { it.clientUpdatedAt }
                } else {
                    movies.sortedBy { it.clientUpdatedAt }
                }
            }
            "vote_average" -> {
                if (isDesc) {
                    movies.sortedByDescending { it.voteAverage ?: 0.0 }
                } else {
                    movies.sortedBy { it.voteAverage ?: 0.0 }
                }
            }
            "personal_rating" -> {
                if (isDesc) {
                    movies.sortedByDescending { it.personalRating ?: 0.0 }
                } else {
                    movies.sortedBy { it.personalRating ?: 0.0 }
                }
            }
            "runtime" -> {
                if (isDesc) {
                    movies.sortedByDescending { getMovieDuration(it) }
                } else {
                    movies.sortedBy { getMovieDuration(it) }
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
