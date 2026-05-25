package com.cinetrack.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.cinetrack.data.api.TMDBSearchResult

@Serializable
data class FindResponse(
    @SerialName("movie_results") val movieResults: List<TMDBSearchResult.MovieResult>? = null,
    @SerialName("tv_results") val tvResults: List<TMDBSearchResult.TvResult>? = null
)
