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
