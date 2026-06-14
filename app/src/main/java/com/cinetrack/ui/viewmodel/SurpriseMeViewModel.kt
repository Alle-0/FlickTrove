package com.cinetrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinetrack.data.Movie
import com.cinetrack.data.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SurpriseTime {
    SHORT, // < 100 min
    MEDIUM, // 100 - 130 min
    LONG, // > 130 min
    ANY
}

enum class SurpriseMood {
    LAUGH, // Comedy, Animation
    TENSION, // Action, Thriller, Horror, Mystery
    EMOTION, // Drama, Romance
    ESCAPE, // Fantasy, Science Fiction, Adventure
    ANY
}

enum class SurpriseCompany {
    ALONE, // Any
    COUPLE, // Romance, Comedy, Drama
    FRIENDS, // Horror, Comedy, Action, Thriller
    FAMILY // Animation, Family, Adventure
}

data class SurpriseMeUiState(
    val unwatchedMovies: List<Movie> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class SurpriseMeViewModel @Inject constructor(
    private val repository: MovieRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SurpriseMeUiState())
    val uiState: StateFlow<SurpriseMeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getLocalMoviesFlow().collect { movies ->
                _uiState.update { 
                    it.copy(
                        unwatchedMovies = movies.filter { movie -> !movie.watched },
                        isLoading = false
                    )
                }
            }
        }
    }

    fun getRandomMovie(): Movie? {
        val movies = _uiState.value.unwatchedMovies
        if (movies.isEmpty()) return null
        return movies.random()
    }

    fun getEmotionalMovie(time: SurpriseTime, mood: SurpriseMood, company: SurpriseCompany): Movie? {
        val movies = _uiState.value.unwatchedMovies
        if (movies.isEmpty()) return null

        // 1. Time Filter
        val timeFiltered = movies.filter { movie ->
            val runtime = movie.runtime ?: 0
            when (time) {
                SurpriseTime.SHORT -> runtime in 1..99
                SurpriseTime.MEDIUM -> runtime in 100..130
                SurpriseTime.LONG -> runtime > 130
                SurpriseTime.ANY -> true
            }
        }.ifEmpty { movies } // Fallback to all movies if time filter returns empty

        // 2. Genre Mappings
        val moodGenres = when (mood) {
            SurpriseMood.LAUGH -> listOf("Comedy", "Commedia", "Animation", "Animazione")
            SurpriseMood.TENSION -> listOf("Action", "Azione", "Thriller", "Horror", "Mystery", "Mistero")
            SurpriseMood.EMOTION -> listOf("Drama", "Dramma", "Romance", "Romantico")
            SurpriseMood.ESCAPE -> listOf("Fantasy", "Science Fiction", "Fantascienza", "Adventure", "Avventura")
            SurpriseMood.ANY -> emptyList()
        }

        val companyGenres = when (company) {
            SurpriseCompany.ALONE -> emptyList()
            SurpriseCompany.COUPLE -> listOf("Romance", "Romantico", "Comedy", "Commedia", "Drama", "Dramma")
            SurpriseCompany.FRIENDS -> listOf("Horror", "Comedy", "Commedia", "Action", "Azione", "Thriller")
            SurpriseCompany.FAMILY -> listOf("Animation", "Animazione", "Family", "Famiglia", "Adventure", "Avventura")
        }

        // 3. Combine Genres (Intersection first, then Union, then Fallback)
        val hasGenre = { movie: Movie, genres: List<String> ->
            val movieGenresStr = movie.genreNamesString ?: ""
            genres.any { g -> 
                movieGenresStr.contains(g, ignoreCase = true) || 
                movie.genres?.any { it.name?.equals(g, ignoreCase = true) == true } == true 
            }
        }

        var candidates = timeFiltered.filter { movie ->
            val matchesMood = moodGenres.isEmpty() || hasGenre(movie, moodGenres)
            val matchesCompany = companyGenres.isEmpty() || hasGenre(movie, companyGenres)
            matchesMood && matchesCompany // Intersection
        }

        if (candidates.isEmpty()) {
            candidates = timeFiltered.filter { movie ->
                val matchesMood = moodGenres.isEmpty() || hasGenre(movie, moodGenres)
                val matchesCompany = companyGenres.isEmpty() || hasGenre(movie, companyGenres)
                matchesMood || matchesCompany // Union
            }
        }

        if (candidates.isEmpty()) {
            candidates = timeFiltered // Fallback to time-filtered only
        }

        return candidates.randomOrNull() ?: movies.randomOrNull() // Ultimate fallback
    }
}
