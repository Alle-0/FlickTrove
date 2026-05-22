package com.cinetrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinetrack.data.Movie
import com.cinetrack.data.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UpdatesUiState(
    val movies: List<Movie> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class UpdatesViewModel @Inject constructor(
    private val repository: MovieRepository
) : ViewModel() {

    val uiState: StateFlow<UpdatesUiState> = repository.getLocalMoviesFlow()
        .map { movies ->
            val updateList = movies.filter { 
                (it.newEpisodesFound ?: 0) > 0 || it.reminder == true 
            }
                .sortedByDescending { it.clientUpdatedAt }
            UpdatesUiState(movies = updateList, isLoading = false)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UpdatesUiState()
        )

    fun clearUpdate(movieId: Long, mediaType: String) {
        viewModelScope.launch {
            val movie = repository.getMovie(movieId, mediaType)
            movie?.let {
                repository.saveMovie(it.copy(newEpisodesFound = 0))
            }
        }
    }

    fun clearMigrated(movieId: Long, mediaType: String) {
        viewModelScope.launch {
            val movie = repository.getMovie(movieId, mediaType)
            movie?.let {
                repository.saveMovie(it.copy(migratedAt = null))
            }
        }
    }

    fun clearAllNewEpisodes() {
        viewModelScope.launch {
            val moviesWithUpdates = uiState.value.movies.filter { (it.newEpisodesFound ?: 0) > 0 }
            moviesWithUpdates.forEach { movie ->
                repository.saveMovie(movie.copy(newEpisodesFound = 0))
            }
        }
    }

    fun clearAllMigrated() {
        viewModelScope.launch {
            val today = java.time.LocalDate.now().toString()
            val moviesWithMigrated = uiState.value.movies.filter { it.migratedAt == today }
            moviesWithMigrated.forEach { movie ->
                repository.saveMovie(movie.copy(migratedAt = null))
            }
        }
    }
}
