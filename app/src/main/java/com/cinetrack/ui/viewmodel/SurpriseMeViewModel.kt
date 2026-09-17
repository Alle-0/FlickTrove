package com.cinetrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinetrack.data.model.Movie
import com.cinetrack.data.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.cinetrack.data.api.TMDBService
import java.io.IOException

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
    val localMovies: List<Movie> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class SurpriseMeViewModel @Inject constructor(
    private val repository: MovieRepository,
    private val tmdbService: TMDBService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SurpriseMeUiState())
    val uiState: StateFlow<SurpriseMeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getLocalMoviesFlow().collect { movies ->
                _uiState.update { 
                    it.copy(
                        localMovies = movies,
                        isLoading = false
                    )
                }
            }
        }
    }

    suspend fun getRandomMovie(): Movie? {
        val localMovies = _uiState.value.localMovies
        val watchedIds = localMovies.filter { it.watched || it.dropped }.map { it.id }.toSet()

        return try {
            val randomPage = (1..20).random()
            val options = mapOf(
                "sort_by" to "popularity.desc",
                "vote_count.gte" to "100",
                "vote_average.gte" to "5.5",
                "include_adult" to "false"
            )
            
            val response = tmdbService.discoverMovies(page = randomPage, options = options)
            val candidates = response.results.filter { it.id !in watchedIds }
            
            candidates.randomOrNull()?.apply { mediaType = "movie" } 
                ?: tmdbService.discoverMovies(page = 1, options = options).results.filter { it.id !in watchedIds }.randomOrNull()?.apply { mediaType = "movie" }
        } catch (e: Exception) {
            val unwatched = localMovies.filter { !it.watched && !it.dropped }
            unwatched.filter { it.mediaType == "movie" }.randomOrNull() ?: unwatched.randomOrNull()
        }
    }

    suspend fun getEmotionalMovie(time: SurpriseTime, mood: SurpriseMood, company: SurpriseCompany): Movie? {
        val localMovies = _uiState.value.localMovies
        val watchedIds = localMovies.filter { it.watched || it.dropped }.map { it.id }.toSet()

        return try {
            // 1. Map Time to TMDB Runtime limits
            val timeOptions = mutableMapOf<String, String>()
            when (time) {
                SurpriseTime.SHORT -> timeOptions["with_runtime.lte"] = "99"
                SurpriseTime.MEDIUM -> {
                    timeOptions["with_runtime.gte"] = "100"
                    timeOptions["with_runtime.lte"] = "130"
                }
                SurpriseTime.LONG -> timeOptions["with_runtime.gte"] = "131"
                SurpriseTime.ANY -> {}
            }

            // 2. Map Genres using TMDB Movie Genre IDs
            val moodGenres = when (mood) {
                SurpriseMood.LAUGH -> "35|16"
                SurpriseMood.TENSION -> "28|53|27|9648"
                SurpriseMood.EMOTION -> "18|10749"
                SurpriseMood.ESCAPE -> "14|878|12"
                SurpriseMood.ANY -> ""
            }

            val companyGenres = when (company) {
                SurpriseCompany.ALONE -> ""
                SurpriseCompany.COUPLE -> "10749|35|18"
                SurpriseCompany.FRIENDS -> "27|35|28|53"
                SurpriseCompany.FAMILY -> "16|10751|12"
            }

            var combinedGenres = ""
            if (moodGenres.isNotEmpty() && companyGenres.isNotEmpty()) {
                combinedGenres = "$moodGenres,$companyGenres" // comma is AND in TMDB
            } else if (moodGenres.isNotEmpty()) {
                combinedGenres = moodGenres
            } else if (companyGenres.isNotEmpty()) {
                combinedGenres = companyGenres
            }

            val baseOptions = mapOf(
                "sort_by" to "popularity.desc",
                "vote_count.gte" to "100",
                "vote_average.gte" to "5.5",
                "include_adult" to "false"
            ) + timeOptions

            val optionsWithGenres = baseOptions.toMutableMap()
            if (combinedGenres.isNotEmpty()) {
                optionsWithGenres["with_genres"] = combinedGenres
            }

            // 3. Two-Step Pagination
            var response = tmdbService.discoverMovies(page = 1, options = optionsWithGenres)
            var totalPages = response.totalPages ?: 1

            if (totalPages == 0 || response.results.isEmpty()) {
                // Loosen constraint: drop time filter
                response = tmdbService.discoverMovies(page = 1, options = baseOptions.toMutableMap().apply { 
                    remove("with_runtime.lte")
                    remove("with_runtime.gte")
                    if (combinedGenres.isNotEmpty()) put("with_genres", combinedGenres)
                })
                totalPages = response.totalPages ?: 1
            }

            if (totalPages > 1) {
                val maxPage = minOf(totalPages, 10)
                val randomPage = (2..maxPage).random()
                val secondResponse = tmdbService.discoverMovies(page = randomPage, options = optionsWithGenres)
                if (secondResponse.results.isNotEmpty()) {
                    response = secondResponse
                }
            }

            val candidates = response.results.filter { it.id !in watchedIds }
            
            candidates.randomOrNull()?.apply { mediaType = "movie" } 
                ?: response.results.filter { it.id !in watchedIds }.randomOrNull()?.apply { mediaType = "movie" }
        } catch (e: Exception) {
            // Offline fallback
            val unwatched = localMovies.filter { !it.watched && !it.dropped }
            unwatched.filter { it.mediaType == "movie" }.randomOrNull() ?: unwatched.randomOrNull()
        }
    }
}
