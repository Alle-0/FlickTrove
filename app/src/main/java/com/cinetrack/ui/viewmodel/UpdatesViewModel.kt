package com.cinetrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinetrack.data.Movie
import com.cinetrack.data.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn

data class UpdatesUiState(
    val movies: ImmutableList<Movie> = persistentListOf(),
    val notificationCount: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class UpdatesViewModel @Inject constructor(
    private val repository: MovieRepository
) : ViewModel() {

    val uiState: StateFlow<UpdatesUiState> = repository.getLocalMoviesFlow()
        .map { movies ->
            val today = java.time.LocalDate.now().toString()
            val updateList = movies.filter { 
                (it.newEpisodesFound ?: 0) > 0 || it.reminder == true || it.migratedAt == today
            }
                .sortedByDescending { it.clientUpdatedAt }
            val unreadNotifCount = movies.count {
                (it.newEpisodesFound ?: 0) > 0 || (it.migratedAt == today && (it.newEpisodesFound ?: 0) == 0)
            }
            UpdatesUiState(
                movies = updateList.toImmutableList(),
                notificationCount = unreadNotifCount,
                isLoading = false
            )
        }
        .flowOn(Dispatchers.Default)
        .catch { e ->
            emit(UpdatesUiState(isLoading = false))
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
            val moviesWithUpdates = repository.getLocalMovies().filter { (it.newEpisodesFound ?: 0) > 0 }
            moviesWithUpdates.forEach { movie ->
                repository.saveMovie(movie.copy(newEpisodesFound = 0))
            }
        }
    }

    fun clearAllMigrated() {
        viewModelScope.launch {
            val today = java.time.LocalDate.now().toString()
            val moviesWithMigrated = repository.getLocalMovies().filter { it.migratedAt == today }
            moviesWithMigrated.forEach { movie ->
                repository.saveMovie(movie.copy(migratedAt = null))
            }
        }
    }
}
