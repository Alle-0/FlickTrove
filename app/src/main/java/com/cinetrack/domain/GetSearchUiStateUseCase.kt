package com.cinetrack.domain

import com.cinetrack.data.Movie
import com.cinetrack.data.api.TMDBSearchResult
import com.cinetrack.data.local.entities.FolderEntity
import com.cinetrack.data.models.SortConfig
import com.cinetrack.data.models.UserPreferences
import com.cinetrack.ui.viewmodel.FilterPill
import com.cinetrack.ui.viewmodel.SearchUiState
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import javax.inject.Inject

class GetSearchUiStateUseCase @Inject constructor() {
    operator fun invoke(
        currentState: SearchUiState,
        rawResults: List<TMDBSearchResult>,
        rawTrendingMovies: List<TMDBSearchResult>,
        rawTrendingTv: List<TMDBSearchResult>,
        rawTrendingPeople: List<TMDBSearchResult>,
        rawDynamicKeywords: List<FilterPill>,
        localMovies: List<Movie>,
        folders: List<FolderEntity>
    ): SearchUiState {
        val results = rawResults.distinctBy { "${it.id}_${it.mediaType}" }
        val trendingMovies = rawTrendingMovies.distinctBy { "${it.id}_${it.mediaType}" }
        val trendingTv = rawTrendingTv.distinctBy { "${it.id}_${it.mediaType}" }
        val trendingPeople = rawTrendingPeople.distinctBy { "${it.id}_${it.mediaType}" }

        val sortConfig = currentState.sortConfig
        val category = currentState.category
        val query = currentState.query

        val filteredResults = results.applyFilter(sortConfig).let { list ->
            val isDesc = sortConfig.sortDirection == "desc"
            val localMoviesMap = localMovies.associateBy { "${it.id}_${it.mediaType}" }
            
            when (sortConfig.sortType) {
                "vote_average" -> {
                    if (isDesc) list.sortedByDescending { it.voteAverage }
                    else list.sortedBy { it.voteAverage }
                }
                "personal_rating" -> {
                    if (isDesc) {
                        list.sortedByDescending { result ->
                            localMoviesMap["${result.id}_${result.mediaType}"]?.personalRating ?: 0.0
                        }
                    } else {
                        list.sortedBy { result ->
                            localMoviesMap["${result.id}_${result.mediaType}"]?.personalRating ?: 0.0
                        }
                    }
                }
                "release_date" -> {
                    if (isDesc) list.sortedByDescending { it.releaseDate }
                    else list.sortedBy { it.releaseDate }
                }
                "title" -> {
                    if (isDesc) list.sortedByDescending { it.title }
                    else list.sortedBy { it.title }
                }
                else -> list
            }
        }

        val movieFolderColors = mutableMapOf<String, MutableList<String>>()
        folders.forEach { folder ->
            val color = folder.color ?: "#FFFFFF"
            folder.itemIds.forEach { itemId ->
                movieFolderColors.getOrPut(itemId) { mutableListOf() }.add(color)
            }
        }
        val immutableMovieFolderColors = movieFolderColors.mapValues { it.value.toImmutableList() }.toImmutableMap()

        val lowerQuery = query.lowercase()
        val availableGenres = if (category == "movie") com.cinetrack.data.GenreConstants.MOVIE_GENRES else com.cinetrack.data.GenreConstants.TV_GENRES
        val keywordToGenre = com.cinetrack.data.KeywordDictionary.italianToTmdbKeywordIds

        val dynamicKeywords = rawDynamicKeywords.filter { it.isKeyword }
            
        val suggestedFilters = mutableListOf<FilterPill>()

        // Always show currently selected genres as pills so the user knows they are active
        sortConfig.selectedGenres.forEach { selectedId ->
            val genre = availableGenres.find { it.id == selectedId }
            if (genre != null) {
                suggestedFilters.add(FilterPill(genre.id, genre.name, isKeyword = false))
            }
        }

        if (query.length >= 2) {
            availableGenres.filter { it.name.lowercase().contains(lowerQuery) }.forEach { genre ->
                if (!suggestedFilters.any { it.id == genre.id }) {
                    suggestedFilters.add(FilterPill(genre.id, genre.name, isKeyword = false))
                }
            }
            keywordToGenre.forEach { (keyword, keywordId) ->
                if (keyword.contains(lowerQuery)) {
                    if (!suggestedFilters.any { it.id == keywordId && it.isKeyword }) {
                        suggestedFilters.add(FilterPill(keywordId, keyword.replaceFirstChar { it.uppercase() }, isKeyword = true))
                    }
                }
            }
            dynamicKeywords.forEach { dynKw ->
                if (!suggestedFilters.any { it.id == dynKw.id }) {
                    suggestedFilters.add(dynKw)
                }
            }
        }

        return currentState.copy(
            results = filteredResults.toImmutableList(),
            trendingMovies = trendingMovies.toImmutableList(),
            trendingTv = trendingTv.toImmutableList(),
            trendingPeople = trendingPeople.toImmutableList(),
            favorites = localMovies.toImmutableList(),
            folders = folders.toImmutableList(),
            movieFolderColors = immutableMovieFolderColors,
            suggestedFilters = suggestedFilters.toImmutableList()
        )
    }
}

fun List<TMDBSearchResult>.applyFilter(config: SortConfig): List<TMDBSearchResult> {
    return this.filter { result ->
        if (result is TMDBSearchResult.PersonResult) return@filter true

        val genreMatch = if (config.selectedGenres.isEmpty()) true else {
            val genreIds = when (result) {
                is TMDBSearchResult.MovieResult -> result.genreIds
                is TMDBSearchResult.TvResult -> result.genreIds
                else -> emptyList()
            }
            config.selectedGenres.any { it.toLong() in genreIds }
        }

        val decadeMatch = if (config.selectedDecades.isEmpty()) true else {
            val date = when (result) {
                is TMDBSearchResult.MovieResult -> result.releaseDate
                is TMDBSearchResult.TvResult -> result.firstAirDate
                else -> null
            }
            val year = try {
                date?.split("-")?.firstOrNull()?.toIntOrNull()
            } catch(e: Exception) { null }
            
            if (year != null) {
                config.selectedDecades.any { decadeStr ->
                    val start = decadeStr.removeSuffix("s").toIntOrNull()
                    start != null && year in start..(start + 9)
                }
            } else false
        }

        genreMatch && decadeMatch
    }
}

val TMDBSearchResult.voteAverage: Double
    get() = when (this) {
        is TMDBSearchResult.MovieResult -> voteAverage ?: 0.0
        is TMDBSearchResult.TvResult -> voteAverage ?: 0.0
        else -> 0.0
    }

val TMDBSearchResult.releaseDate: String
    get() = when (this) {
        is TMDBSearchResult.MovieResult -> releaseDate ?: ""
        is TMDBSearchResult.TvResult -> firstAirDate ?: ""
        else -> ""
    }

val TMDBSearchResult.title: String
    get() = when (this) {
        is TMDBSearchResult.MovieResult -> title ?: ""
        is TMDBSearchResult.TvResult -> name ?: ""
        is TMDBSearchResult.PersonResult -> name
    }
