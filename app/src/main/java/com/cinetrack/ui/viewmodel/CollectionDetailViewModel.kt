package com.cinetrack.ui.viewmodel

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.Coil
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.cinetrack.data.api.CollectionResponse
import com.cinetrack.data.model.Movie
import com.cinetrack.data.model.UserPreferences
import com.cinetrack.data.repository.MovieRepository
import com.cinetrack.data.repository.PreferenceRepository
import com.cinetrack.domain.CycleMovieStatusUseCase
import com.cinetrack.ui.utils.ActionFeedbackManager
import com.cinetrack.ui.utils.ColorUtils
import com.cinetrack.ui.utils.ErrorMapper
import com.cinetrack.ui.utils.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CollectionDetailUiState(
    val collectionId: Long = 0L,
    val collectionName: String? = null,
    val collection: CollectionResponse? = null,
    val isLoading: Boolean = true,
    val favorites: ImmutableList<Movie> = persistentListOf(),
    val folders: ImmutableList<com.cinetrack.data.local.entities.FolderEntity> = persistentListOf(),
    val movieFolderColors: ImmutableMap<String, ImmutableList<String>> = persistentMapOf(),
    val preferences: UserPreferences = UserPreferences(),
    val error: String? = null
)

@HiltViewModel
class CollectionDetailViewModel @Inject constructor(
    private val cycleMovieStatusUseCase: CycleMovieStatusUseCase,
    private val repository: MovieRepository,
    private val preferenceRepository: PreferenceRepository,
    private val actionFeedbackManager: ActionFeedbackManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _collectionId = MutableStateFlow<Long>(0L)
    private var _collectionName: String? = null
    private val _isLoading = MutableStateFlow(true)
    private val _collection = MutableStateFlow<CollectionResponse?>(null)
    private val _error = MutableStateFlow<String?>(null)
    private val _extractedColor = MutableStateFlow<Color?>(null)
    val extractedColor: StateFlow<Color?> = _extractedColor.asStateFlow()

    val scrollState = androidx.compose.foundation.ScrollState(0)
    val animatedMovieIds = mutableSetOf<String>()

    fun fetchAccentColor(imageUrl: String, forceReload: Boolean = false) {
        if (forceReload || _extractedColor.value == null) {
            viewModelScope.launch(Dispatchers.Default) {
                try {
                    val loader = Coil.imageLoader(context)
                    val request = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .allowHardware(false)
                        .build()

                    val result = loader.execute(request)
                    if (result is SuccessResult) {
                        val bitmap = result.drawable.toBitmap()
                        if (bitmap.width > 0 && bitmap.height > 0) {
                            val rawColor = ColorUtils.extractAverageColor(bitmap)
                            if (rawColor != Color.Unspecified) {
                                val ambientColor = ColorUtils.darkenForAmbient(rawColor)
                                val finalColor = ColorUtils.ensureMinimumLuminance(ambientColor, 0.25f)
                                _extractedColor.value = finalColor
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }

    fun initCollection(id: Long, name: String?) {
        if (_collectionId.value == 0L && id != 0L) {
            _collectionId.value = id
            _collectionName = name
            fetchCollection(id)
        }
    }

    fun emitMessage(message: UiText) {
        actionFeedbackManager.emit(message)
    }

    val uiState: StateFlow<CollectionDetailUiState> = combine(
        combine(
            _collection,
            _isLoading,
            _error
        ) { col, loading, err -> Triple(col, loading, err) },
        combine(
            repository.getLocalMoviesFlow(),
            repository.getFoldersFlow(),
            preferenceRepository.userPreferencesFlow
        ) { favs, flds, prefs -> Triple(favs, flds, prefs) }
    ) { groupA, groupB ->
        val collection = groupA.first
        val isLoading = groupA.second
        val error = groupA.third

        val favorites = groupB.first
        val folders = groupB.second
        val preferences = groupB.third

        val folderColorsMap = mutableMapOf<String, ImmutableList<String>>()
        folders.forEach { folder ->
            val color = folder.color ?: "#FFFFFF"
            folder.itemIds.forEach { itemId ->
                val list = folderColorsMap[itemId] ?: persistentListOf()
                folderColorsMap[itemId] = (list + color).toImmutableList()
            }
        }

        CollectionDetailUiState(
            collectionId = _collectionId.value,
            collectionName = _collectionName ?: collection?.name,
            collection = collection,
            isLoading = isLoading,
            favorites = favorites.toImmutableList(),
            folders = folders.toImmutableList(),
            movieFolderColors = folderColorsMap.toImmutableMap(),
            preferences = preferences,
            error = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CollectionDetailUiState()
    )

    private fun fetchCollection(id: Long) {
        _collectionId.value = id
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val details = repository.fetchCollectionDetails(id)
                // Sort parts by release date chronologically so saga films are in correct viewing sequence!
                val sortedParts = details.parts.sortedBy { movie ->
                    movie.releaseDate?.takeIf { it.isNotBlank() } ?: "9999-99-99"
                }
                _collection.value = details.copy(parts = sortedParts)
            } catch (e: Exception) {
                _error.value = ErrorMapper.map(e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun retry() {
        if (_collectionId.value != 0L) {
            fetchCollection(_collectionId.value)
        }
    }

    fun toggleFavorite(movie: Movie) {
        viewModelScope.launch {
            cycleMovieStatusUseCase(movie)
        }
    }

    fun updateRating(movie: Movie, rating: Double) {
        viewModelScope.launch {
            val local = repository.getMovie(movie.id, movie.mediaType)
            val current = local ?: movie
            repository.saveMovie(current.copy(personalRating = rating))
        }
    }

    fun updateNote(movie: Movie, note: String) {
        viewModelScope.launch {
            val local = repository.getMovie(movie.id, movie.mediaType)
            val current = local ?: movie
            repository.saveMovie(current.copy(personalNote = note))
        }
    }

    fun toggleItemInFolder(folder: com.cinetrack.data.local.entities.FolderEntity, movie: Movie) {
        viewModelScope.launch {
            val compositeId = "${movie.mediaType}_${movie.id}"
            val newItemIds = if (folder.itemIds.contains(compositeId)) {
                folder.itemIds - compositeId
            } else {
                folder.itemIds + compositeId
            }
            repository.saveFolder(folder.copy(itemIds = newItemIds, updatedAt = java.time.Instant.now().toString()))
        }
    }
}
