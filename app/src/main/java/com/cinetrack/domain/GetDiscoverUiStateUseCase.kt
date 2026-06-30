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
        

        val movieFolderColors = mutableMapOf<String, ImmutableList<String>>()
        folders.forEach { folder ->
            val color = folder.color ?: "#FFFFFF"
            folder.itemIds.forEach { itemId ->
                val colors = movieFolderColors[itemId] ?: persistentListOf()
                movieFolderColors[itemId] = (colors + color).toImmutableList()
            }
        }

        return DiscoverUiState(
            movies = syncedMovies.toImmutableList(),
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
