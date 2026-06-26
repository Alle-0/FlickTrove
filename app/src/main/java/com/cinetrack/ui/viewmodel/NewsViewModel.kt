package com.cinetrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinetrack.data.model.NewsItem
import com.cinetrack.data.repository.NewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import com.cinetrack.data.repository.PreferenceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged

sealed class NewsUiState {
    object Loading : NewsUiState()
    data class Success(val news: List<NewsItemUi>) : NewsUiState()
    data class Error(val message: String) : NewsUiState()
}

data class NewsItemUi(
    val title: String,
    val link: String,
    val imageUrl: String?,
    val formattedDate: String,
    val source: String
)

@HiltViewModel
class NewsViewModel @Inject constructor(
    private val newsRepository: NewsRepository,
    private val preferenceRepository: PreferenceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<NewsUiState>(NewsUiState.Loading)
    val uiState: StateFlow<NewsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferenceRepository.userPreferencesFlow
                .map { it.contentLanguage }
                .distinctUntilChanged()
                .collect {
                    fetchNews()
                }
        }
    }

    fun fetchNews() {
        viewModelScope.launch {
            _uiState.value = NewsUiState.Loading
            try {
                val items = newsRepository.getNews()
                val uiItems = items.map { item ->
                    val source = when {
                        item.link.contains("screenrant.com") -> "ScreenRant"
                        item.link.contains("collider.com") -> "Collider"
                        item.link.contains("everyeye.it") -> "Everyeye"
                        item.link.contains("movieplayer.it") -> "Movieplayer.it"
                        else -> "News"
                    }
                    NewsItemUi(
                        title = item.title,
                        link = item.link,
                        imageUrl = item.imageUrl,
                        formattedDate = formatRfc822Date(item.pubDate),
                        source = source
                    )
                }
                _uiState.value = NewsUiState.Success(uiItems)
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = NewsUiState.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    private fun formatRfc822Date(dateString: String): String {
        if (dateString.isBlank()) return ""
        return try {
            // ScreenRant & Movieplayer usually use RFC-822 or RFC-1123: "Fri, 26 Jun 2026 11:24:21 +0000"
            val formats = listOf(
                SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH),
                SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH),
                SimpleDateFormat("dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
            )
            
            var parsedDate: Date? = null
            for (format in formats) {
                try {
                    parsedDate = format.parse(dateString)
                    if (parsedDate != null) break
                } catch (e: Exception) {
                    // Ignore and try next
                }
            }
            
            if (parsedDate != null) {
                // Convert to a clean UI format like "26 Giu, 11:24" based on local Locale
                val outFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
                outFormat.format(parsedDate)
            } else {
                // Fallback to original
                dateString
            }
        } catch (e: Exception) {
            dateString
        }
    }
}
