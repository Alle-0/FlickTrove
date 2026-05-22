package com.cinetrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinetrack.data.Movie
import com.cinetrack.data.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SurpriseMeUiState(
    val randomMovie: Movie? = null,
    val hasNoMovies: Boolean = false
)

@HiltViewModel
class SurpriseMeViewModel @Inject constructor(
    private val repository: MovieRepository
) : ViewModel() {

    val uiState: StateFlow<SurpriseMeUiState> = repository.getLocalMoviesFlow()
        .map { movies ->
            val toWatch = movies.filter { movie: Movie -> !movie.watched }
            if (toWatch.isNotEmpty()) {
                SurpriseMeUiState(randomMovie = toWatch.random())
            } else {
                SurpriseMeUiState(hasNoMovies = true)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SurpriseMeUiState()
        )
}
