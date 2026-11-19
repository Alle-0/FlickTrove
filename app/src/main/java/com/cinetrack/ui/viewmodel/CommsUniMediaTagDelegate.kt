package com.cinetrack.ui.viewmodel

import com.cinetrack.data.api.TMDBSearchResult
import com.cinetrack.data.model.AppComment
import com.cinetrack.data.repository.MovieRepository
import com.cinetrack.ui.utils.TaggedMediaRef
import com.cinetrack.ui.utils.extractTmdbMediaRefsFromText
import com.cinetrack.ui.utils.resolveTmdbMediaTagsMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import java.util.Collections
import javax.inject.Inject

/**
 * Delegate responsible for CommsUni media hashtag parsing, TMDB/local search autocomplete,
 * and background resolution of referenced media titles and posters.
 */
class CommsUniMediaTagDelegate @Inject constructor(
    private val movieRepository: MovieRepository
) {
    private val _mediaTagSuggestions = MutableStateFlow<List<TaggedMediaRef>>(emptyList())
    val mediaTagSuggestions: StateFlow<List<TaggedMediaRef>> = _mediaTagSuggestions.asStateFlow()

    private val _isSearchingMediaTags = MutableStateFlow(false)
    val isSearchingMediaTags: StateFlow<Boolean> = _isSearchingMediaTags.asStateFlow()

    private val _taggedMediaTitles = MutableStateFlow<Map<String, String>>(emptyMap())
    val taggedMediaTitles: StateFlow<Map<String, String>> = _taggedMediaTitles.asStateFlow()

    private val _taggedMediaPosters = MutableStateFlow<Map<String, String>>(emptyMap())
    val taggedMediaPosters: StateFlow<Map<String, String>> = _taggedMediaPosters.asStateFlow()

    private var mediaTagSearchJob: Job? = null
    private val resolvedOrInFlightMediaTags = Collections.synchronizedSet(mutableSetOf<String>())
    private val mediaTagSemaphore = Semaphore(4)

    fun searchMediaTags(query: String, scope: CoroutineScope) {
        val cleanQuery = query.trim()
        mediaTagSearchJob?.cancel()
        if (cleanQuery.length < 2) {
            _mediaTagSuggestions.value = emptyList()
            _isSearchingMediaTags.value = false
            return
        }

        mediaTagSearchJob = scope.launch {
            _isSearchingMediaTags.value = true
            delay(300)
            val results = withContext(Dispatchers.IO) {
                try {
                    val tmdbResults = movieRepository.searchMulti(cleanQuery, page = 1)
                        .mapNotNull { item ->
                            when (item) {
                                is TMDBSearchResult.MovieResult -> {
                                    val title = item.title?.trim().orEmpty()
                                    if (title.isEmpty()) null else TaggedMediaRef(
                                        id = item.id,
                                        mediaType = "movie",
                                        title = title,
                                        posterPath = item.posterPath,
                                        releaseYear = item.releaseDate?.take(4)?.takeIf { it.isNotBlank() }
                                    )
                                }
                                is TMDBSearchResult.TvResult -> {
                                    val name = item.name?.trim().orEmpty()
                                    if (name.isEmpty()) null else TaggedMediaRef(
                                        id = item.id,
                                        mediaType = "tv",
                                        title = name,
                                        posterPath = item.posterPath,
                                        releaseYear = item.firstAirDate?.take(4)?.takeIf { it.isNotBlank() }
                                    )
                                }
                                else -> null
                            }
                        }
                        .take(8)
                    if (tmdbResults.isNotEmpty()) {
                        tmdbResults
                    } else {
                        searchLocalMediaTags(cleanQuery)
                    }
                } catch (_: Exception) {
                    searchLocalMediaTags(cleanQuery)
                }
            }
            _mediaTagSuggestions.value = results
            _isSearchingMediaTags.value = false
        }
    }

    private suspend fun searchLocalMediaTags(cleanQuery: String): List<TaggedMediaRef> {
        return try {
            movieRepository.searchLocalMovies(cleanQuery)
                .mapNotNull { movie ->
                    val title = (movie.title ?: movie.name)?.trim().orEmpty()
                    if (title.isEmpty()) null else TaggedMediaRef(
                        id = movie.id,
                        mediaType = if (movie.mediaType == "tv") "tv" else "movie",
                        title = title,
                        posterPath = movie.posterPath,
                        releaseYear = (movie.releaseYear ?: movie.releaseDate ?: movie.firstAirDate)?.take(4)?.takeIf { it.isNotBlank() }
                    )
                }
                .take(8)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun clearMediaTagSuggestions() {
        mediaTagSearchJob?.cancel()
        _mediaTagSuggestions.value = emptyList()
        _isSearchingMediaTags.value = false
    }

    fun resolveMediaTagsForComments(comments: List<AppComment>, scope: CoroutineScope) {
        val refs = comments
            .flatMap { extractTmdbMediaRefsFromText(it.text) }
            .distinct()
            .filter { (type, id) ->
                val key = "$type:$id"
                !_taggedMediaTitles.value.containsKey(key) && resolvedOrInFlightMediaTags.add(key)
            }
        if (refs.isEmpty()) return

        scope.launch(Dispatchers.IO) {
            val resolved = resolveTmdbMediaTagsMetadata(
                refs = refs,
                movieRepository = movieRepository,
                semaphore = mediaTagSemaphore
            )
            if (resolved.titles.isNotEmpty()) {
                _taggedMediaTitles.value = _taggedMediaTitles.value + resolved.titles
            }
            if (resolved.posters.isNotEmpty()) {
                _taggedMediaPosters.value = _taggedMediaPosters.value + resolved.posters
            }
        }
    }

    fun cancel() {
        mediaTagSearchJob?.cancel()
    }
}
