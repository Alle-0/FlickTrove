package com.cinetrack.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinetrack.data.model.EditorialCollection
import com.cinetrack.data.model.Movie
import com.cinetrack.data.repository.EditorialCollectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditorialCollectionUiState(
    val isLoading: Boolean = false,
    val collection: EditorialCollection? = null,
    val movies: List<Pair<Movie, Int>> = emptyList(), // Pair of Movie and timelineIndex
    val isChronologicalOrder: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class EditorialCollectionViewModel @Inject constructor(
    private val repository: EditorialCollectionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorialCollectionUiState())
    val uiState: StateFlow<EditorialCollectionUiState> = _uiState.asStateFlow()

    fun loadCollection(collectionId: String) {
        if (_uiState.value.collection?.id == collectionId) return
        
        _uiState.update { it.copy(isLoading = true, error = null) }
        
        viewModelScope.launch {
            try {
                val collection = repository.getCollection(collectionId)
                if (collection != null) {
                    val movies = repository.getCollectionMovies(collectionId)
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            collection = collection,
                            movies = sortMovies(movies, it.isChronologicalOrder)
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Collection not found") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun toggleSortOrder() {
        _uiState.update { state ->
            val newOrder = !state.isChronologicalOrder
            state.copy(
                isChronologicalOrder = newOrder,
                movies = sortMovies(state.movies, newOrder)
            )
        }
    }

    private fun sortMovies(movies: List<Pair<Movie, Int>>, isChronological: Boolean): List<Pair<Movie, Int>> {
        return if (isChronological) {
            movies.sortedBy { it.second } // Sort by timelineIndex
        } else {
            movies.sortedBy { it.first.releaseDate } // Sort by release date
        }
    }
}
