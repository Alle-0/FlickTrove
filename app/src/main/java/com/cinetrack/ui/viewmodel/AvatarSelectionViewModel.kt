package com.cinetrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.cinetrack.data.repository.MovieRepository
import com.cinetrack.data.repository.TvdbRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.cinetrack.data.repository.StorageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.cinetrack.data.api.TMDBSearchResult
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.utils.FuzzySearch
import kotlinx.coroutines.flow.first

@HiltViewModel
class AvatarSelectionViewModel @Inject constructor(
    val movieRepository: MovieRepository,
    val tvdbRepository: TvdbRepository,
    private val storageRepository: StorageRepository
) : ViewModel() {
    
    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _uploadError = MutableStateFlow<String?>(null)
    val uploadError: StateFlow<String?> = _uploadError.asStateFlow()

    fun uploadCustomAvatar(uri: Uri, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            _isUploading.value = true
            _uploadError.value = null
            
            val result = storageRepository.uploadAvatar(uri)
            
            _isUploading.value = false
            result.onSuccess { downloadUrl ->
                onSuccess(downloadUrl)
            }.onFailure { exception ->
                _uploadError.value = exception.localizedMessage ?: "Failed to upload image."
            }
        }
    }

    suspend fun searchMulti(query: String): List<TMDBSearchResult> {
        val trimmed = query.trim()
        if (trimmed.length < 3) return emptyList()

        // 1. Check local library with fuzzy search
        val localMovies = try {
            movieRepository.getLocalMoviesFlow().first()
        } catch (e: Exception) {
            emptyList()
        }

        val localFuzzyMatches = localMovies.filter { movie ->
            val title = movie.title ?: ""
            val name = movie.name ?: ""
            title.contains(trimmed, ignoreCase = true) ||
            name.contains(trimmed, ignoreCase = true) ||
            FuzzySearch.matchesFuzzy(trimmed, title) ||
            FuzzySearch.matchesFuzzy(trimmed, name)
        }.map { movie ->
            if (movie.mediaType == "tv") movie.toTvResult() else movie.toMovieResult()
        }

        // 2. Direct TMDB search
        var tmdbResults = try {
            movieRepository.searchMulti(trimmed).filter {
                it is TMDBSearchResult.MovieResult || it is TMDBSearchResult.TvResult
            }
        } catch (e: Exception) {
            emptyList()
        }

        // 3. Fallback TMDB search when results are empty or few (<= 3) and query >= 4
        // (Compensates for TMDB's strict spelling, e.g. "interstelar" -> "interstellar")
        if (tmdbResults.size <= 3 && trimmed.length >= 4) {
            val fallbackQuery = FuzzySearch.buildFallbackQuery(trimmed)
            if (fallbackQuery != null && fallbackQuery != trimmed) {
                try {
                    val fallbackBatch = movieRepository.searchMulti(fallbackQuery).filter {
                        it is TMDBSearchResult.MovieResult || it is TMDBSearchResult.TvResult
                    }
                    val scoredFallback = fallbackBatch.filter { res ->
                        FuzzySearch.score(trimmed, res.displayTitle) >= 0.70
                    }
                    if (scoredFallback.isNotEmpty()) {
                        tmdbResults = (tmdbResults + scoredFallback).distinctBy { "${it.id}_${it.mediaType}" }
                    }
                } catch (e: Exception) {
                    // Ignore fallback failure
                }
            }
        }

        // 4. Merge local matches and TMDB results, prioritized by local presence and similarity
        return (localFuzzyMatches + tmdbResults)
            .distinctBy { "${it.id}_${it.mediaType}" }
            .sortedByDescending { res ->
                val isLocal = localFuzzyMatches.any { it.id == res.id && it.mediaType == res.mediaType }
                val simScore = FuzzySearch.score(trimmed, res.displayTitle)
                val voteAvg = when (res) {
                    is TMDBSearchResult.MovieResult -> res.voteAverage ?: 0.0
                    is TMDBSearchResult.TvResult -> res.voteAverage ?: 0.0
                    else -> 0.0
                }
                val baseScore = if (isLocal) 200.0 else 0.0
                baseScore + (simScore * 100.0) + voteAvg
            }
    }

    private fun Movie.toMovieResult() = TMDBSearchResult.MovieResult(
        id = id,
        title = title,
        posterPath = posterPath,
        backdropPath = backdropPath,
        voteAverage = voteAverage,
        releaseDate = releaseDate,
        genreIds = genreIds?.map { it.toLong() } ?: emptyList(),
        overview = overview
    )

    private fun Movie.toTvResult() = TMDBSearchResult.TvResult(
        id = id,
        name = name,
        posterPath = posterPath,
        backdropPath = backdropPath,
        voteAverage = voteAverage,
        firstAirDate = firstAirDate,
        genreIds = genreIds?.map { it.toLong() } ?: emptyList(),
        overview = overview
    )
    suspend fun getMovieDetails(id: Long) = movieRepository.fetchMovieDetails(id, isTv = false)
    suspend fun getTVDetails(id: Long) = movieRepository.fetchMovieDetails(id, isTv = true)
    
    suspend fun getMovieCharacterImages(title: String, year: String): Map<String, String> {
        return tvdbRepository.getMovieCharacterImagesMap(title, year)
    }
    
    suspend fun getSeriesCharacterImages(tvdbId: String): Map<String, String> {
        return tvdbRepository.getSeriesCharacterImagesMap(tvdbId)
    }
}
