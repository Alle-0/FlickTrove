package com.cinetrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinetrack.data.model.GiphyItem
import com.cinetrack.data.repository.GiphyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GiphyPickerUiState(
    val query: String = "",
    val items: List<GiphyItem> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val hasMore: Boolean = true,
    val isSearching: Boolean = false
)

@HiltViewModel
class GiphyViewModel @Inject constructor(
    private val repository: GiphyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GiphyPickerUiState())
    val uiState = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var currentPage = 0
    private val PAGE_SIZE = 20

    init {
        loadTrending()
    }

    fun loadTrending() {
        searchJob?.cancel()
        currentPage = 0
        _uiState.update { it.copy(query = "", isSearching = false, isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = repository.getTrending(limit = PAGE_SIZE, offset = 0)
            result.onSuccess { items ->
                _uiState.update { it.copy(items = items, isLoading = false, hasMore = items.size >= PAGE_SIZE) }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, errorMessage = err.localizedMessage) }
            }
        }
    }

    fun onQueryChanged(newQuery: String) {
        val trimmed = newQuery.trim()
        _uiState.update { it.copy(query = newQuery) }

        searchJob?.cancel()

        if (trimmed.isEmpty()) {
            loadTrending()
            return
        }

        if (trimmed.length < 2) {
            // Avoid wasteful API queries on a single character
            return
        }

        searchJob = viewModelScope.launch {
            // Aggressive debounce: 600 ms
            delay(600L)
            currentPage = 0
            _uiState.update { it.copy(isSearching = true, isLoading = true, errorMessage = null) }
            val result = repository.searchGifs(query = trimmed, limit = PAGE_SIZE, offset = 0)
            result.onSuccess { items ->
                _uiState.update { it.copy(items = items, isLoading = false, hasMore = items.size >= PAGE_SIZE) }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, errorMessage = err.localizedMessage) }
            }
        }
    }

    fun loadNextPage() {
        val currentState = _uiState.value
        if (currentState.isLoading || currentState.isLoadingMore || !currentState.hasMore) return

        val nextPage = currentPage + 1
        val offset = nextPage * PAGE_SIZE

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            val result = if (currentState.isSearching) {
                repository.searchGifs(query = currentState.query.trim(), limit = PAGE_SIZE, offset = offset)
            } else {
                repository.getTrending(limit = PAGE_SIZE, offset = offset)
            }

            result.onSuccess { newItems ->
                currentPage = nextPage
                _uiState.update {
                    it.copy(
                        items = it.items + newItems,
                        isLoadingMore = false,
                        hasMore = newItems.size >= PAGE_SIZE
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    fun retry() {
        val currentState = _uiState.value
        if (currentState.isSearching && currentState.query.isNotBlank()) {
            onQueryChanged(currentState.query)
        } else {
            loadTrending()
        }
    }
}
