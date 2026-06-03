package com.cinetrack.domain

import com.cinetrack.data.Movie
import com.cinetrack.data.local.entities.FolderEntity
import com.cinetrack.data.models.SortConfig
import com.cinetrack.data.models.UserPreferences
import com.cinetrack.ui.viewmodel.DiscoverUiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.persistentListOf
import javax.inject.Inject

class GetDiscoverUiStateUseCase @Inject constructor() {
    operator fun invoke(
        apiMovies: List<Movie>,
        isLoading: Boolean,
        isNextPageLoading: Boolean,
        currentPage: Int,
        isEndReached: Boolean,
        localMovies: List<Movie>,
        sortConfig: SortConfig,
        folders: List<FolderEntity>,
        prefs: UserPreferences,
        type: String,
        genreName: String?
    ): DiscoverUiState {
        // Sync api movies with local state
        val syncedMovies = apiMovies.map { apiMovie ->
            localMovies.find { it.id == apiMovie.id && it.mediaType == apiMovie.mediaType } ?: apiMovie
        }
        
        // Apply local filtering
        val filteredMovies = syncedMovies.filter { movie ->
            val matchesGenres = if (sortConfig.selectedGenres.isEmpty()) true 
                else movie.genreIds?.any { genreId -> sortConfig.selectedGenres.contains(genreId.toLong()) } ?: false
            
            val matchesProviders = if (sortConfig.selectedProviders.isEmpty()) true
                else movie.streamingProviderIds?.any { providerId -> sortConfig.selectedProviders.contains(providerId) } ?: false
                
            val matchesDecade = if (sortConfig.selectedDecades.isEmpty()) true
                else {
                    val year = movie.releaseDate?.split("-")?.firstOrNull()?.toIntOrNull()
                    if (year != null) {
                        sortConfig.selectedDecades.any { decade ->
                            try {
                                val decadeStart = decade.toInt()
                                year in decadeStart..(decadeStart + 9)
                            } catch (e: Exception) {
                                false
                            }
                        }
                    } else false
                }
                
            val isUpcomingSection = type.contains("upcoming") || type.contains("airing_today") || type.contains("on_the_air")
            val meetsReleaseCriteria = isUpcomingSection || movie.isReleased
                
            matchesGenres && matchesProviders && matchesDecade && meetsReleaseCriteria
        }
        
        // Apply local sorting
        val sortedMovies = when (sortConfig.sortType) {
            "release_date" -> {
                if (sortConfig.sortDirection == "desc") filteredMovies.sortedByDescending { it.releaseDate ?: "" }
                else filteredMovies.sortedBy { it.releaseDate ?: "9999-99-99" }
            }
            "vote_average" -> {
                if (sortConfig.sortDirection == "desc") filteredMovies.sortedByDescending { it.voteAverage ?: 0.0 }
                else filteredMovies.sortedBy { it.voteAverage ?: 10.0 }
            }
            "title" -> {
                if (sortConfig.sortDirection == "desc") filteredMovies.sortedByDescending { it.title ?: it.name ?: "" }
                else filteredMovies.sortedBy { it.title ?: it.name ?: "" }
            }
            else -> filteredMovies
        }

        val movieFolderColors = mutableMapOf<String, ImmutableList<String>>()
        folders.forEach { folder ->
            val color = folder.color ?: "#FFFFFF"
            folder.itemIds.forEach { itemId ->
                val colors = movieFolderColors[itemId] ?: persistentListOf()
                movieFolderColors[itemId] = (colors + color).toImmutableList()
            }
        }

        return DiscoverUiState(
            movies = sortedMovies.toImmutableList(),
            isLoading = isLoading,
            isNextPageLoading = isNextPageLoading,
            type = type,
            genreName = genreName,
            currentPage = currentPage,
            isEndReached = isEndReached,
            favorites = localMovies.toImmutableList(),
            sortConfig = sortConfig,
            movieFolderColors = movieFolderColors.toImmutableMap(),
            folders = folders.toImmutableList(),
            preferences = prefs
        )
    }
}
