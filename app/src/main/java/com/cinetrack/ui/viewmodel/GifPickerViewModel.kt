package com.cinetrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinetrack.data.model.GifItem
import com.cinetrack.data.model.KlipyMediaType
import com.cinetrack.data.repository.KlipyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GifPickerUiState(
    val selectedType: KlipyMediaType = KlipyMediaType.GIF,
    val query: String = "",
    val items: List<GifItem> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val hasMore: Boolean = true,
    val isSearching: Boolean = false
)

@HiltViewModel
class GifPickerViewModel @Inject constructor(
    private val repository: KlipyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GifPickerUiState())
    val uiState = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var loadMoreJob: Job? = null
    private var currentPage = 1
    private val pageSize = 20

    init {
        loadTrending(mediaType = KlipyMediaType.GIF, clearQuery = true)
    }

    fun selectMediaType(mediaType: KlipyMediaType) {
        val currentState = _uiState.value
        if (currentState.selectedType == mediaType) return

        loadJob?.cancel()
        loadMoreJob?.cancel()
        currentPage = 1

        val trimmedQuery = currentState.query.trim()
        if (trimmedQuery.length >= 2) {
            executeSearch(query = trimmedQuery, mediaType = mediaType, debounceMs = 0L)
        } else {
            loadTrending(mediaType = mediaType, clearQuery = false)
        }
    }

    fun loadTrending(
        mediaType: KlipyMediaType = _uiState.value.selectedType,
        clearQuery: Boolean = true
    ) {
        loadJob?.cancel()
        loadMoreJob?.cancel()
        currentPage = 1
        _uiState.update {
            it.copy(
                selectedType = mediaType,
                query = if (clearQuery) "" else it.query,
                items = emptyList(),
                isSearching = false,
                isLoading = true,
                isLoadingMore = false,
                errorMessage = null,
                hasMore = true
            )
        }
        loadJob = viewModelScope.launch {
            val result = repository.getTrending(page = 1, perPage = pageSize, mediaType = mediaType)
            if (_uiState.value.selectedType != mediaType) return@launch
            result.onSuccess { pageResult ->
                _uiState.update {
                    it.copy(
                        items = pageResult.items,
                        isLoading = false,
                        hasMore = pageResult.hasNext
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, errorMessage = err.localizedMessage ?: "Error") }
            }
        }
    }

    fun onQueryChanged(newQuery: String) {
        val trimmed = newQuery.trim()
        val activeType = _uiState.value.selectedType
        _uiState.update { it.copy(query = newQuery) }

        loadJob?.cancel()
        loadMoreJob?.cancel()

        if (trimmed.isEmpty()) {
            loadTrending(mediaType = activeType, clearQuery = true)
            return
        }

        if (trimmed.length < 2) {
            return
        }

        executeSearch(query = trimmed, mediaType = activeType, debounceMs = 600L)
    }

    private fun executeSearch(
        query: String,
        mediaType: KlipyMediaType,
        debounceMs: Long
    ) {
        if (debounceMs == 0L) {
            currentPage = 1
            _uiState.update {
                it.copy(
                    selectedType = mediaType,
                    items = emptyList(),
                    isSearching = true,
                    isLoading = true,
                    isLoadingMore = false,
                    errorMessage = null,
                    hasMore = true
                )
            }
        } else {
            _uiState.update { it.copy(selectedType = mediaType) }
        }

        loadJob = viewModelScope.launch {
            if (debounceMs > 0L) {
                delay(debounceMs)
                currentPage = 1
                _uiState.update {
                    it.copy(
                        isSearching = true,
                        isLoading = true,
                        isLoadingMore = false,
                        errorMessage = null
                    )
                }
            }
            val result = repository.searchGifs(
                query = query,
                page = 1,
                perPage = pageSize,
                mediaType = mediaType
            )
            if (_uiState.value.selectedType != mediaType) return@launch
            result.onSuccess { pageResult ->
                _uiState.update {
                    it.copy(
                        items = pageResult.items,
                        isLoading = false,
                        hasMore = pageResult.hasNext
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, errorMessage = err.localizedMessage ?: "Error") }
            }
        }
    }

    fun loadNextPage() {
        val currentState = _uiState.value
        if (currentState.isLoading || currentState.isLoadingMore || !currentState.hasMore) return

        val targetType = currentState.selectedType
        val nextPage = currentPage + 1

        loadMoreJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            val result = if (currentState.isSearching) {
                repository.searchGifs(
                    query = currentState.query.trim(),
                    page = nextPage,
                    perPage = pageSize,
                    mediaType = targetType
                )
            } else {
                repository.getTrending(page = nextPage, perPage = pageSize, mediaType = targetType)
            }

            if (_uiState.value.selectedType != targetType) return@launch
            result.onSuccess { pageResult ->
                currentPage = nextPage
                _uiState.update {
                    val merged = (it.items + pageResult.items).distinctBy { item -> item.id }
                    it.copy(
                        items = merged,
                        isLoadingMore = false,
                        hasMore = pageResult.hasNext
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    fun onGifSelected(item: GifItem) {
        val activeQuery = _uiState.value.takeIf { it.isSearching }?.query?.trim()
        viewModelScope.launch {
            repository.registerShare(slug = item.slug, query = activeQuery, mediaType = item.type)
        }
    }

    fun retry() {
        val currentState = _uiState.value
        val trimmed = currentState.query.trim()
        if (currentState.isSearching && trimmed.length >= 2) {
            executeSearch(query = trimmed, mediaType = currentState.selectedType, debounceMs = 0L)
        } else {
            loadTrending(mediaType = currentState.selectedType, clearQuery = false)
        }
    }
}
