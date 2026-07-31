package com.cinetrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.cinetrack.data.repository.MovieRepository
import com.cinetrack.data.repository.TvdbRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AvatarSelectionViewModel @Inject constructor(
    val movieRepository: MovieRepository,
    val tvdbRepository: TvdbRepository
) : ViewModel() {
    suspend fun searchMulti(query: String) = movieRepository.searchMulti(query)
    suspend fun getMovieDetails(id: Long) = movieRepository.fetchMovieDetails(id, isTv = false)
    suspend fun getTVDetails(id: Long) = movieRepository.fetchMovieDetails(id, isTv = true)
    
    suspend fun getMovieCharacterImages(title: String, year: String): Map<String, String> {
        return tvdbRepository.getMovieCharacterImagesMap(title, year)
    }
    
    suspend fun getSeriesCharacterImages(tvdbId: String): Map<String, String> {
        return tvdbRepository.getSeriesCharacterImagesMap(tvdbId)
    }
}
