package com.cinetrack.data.model

import com.cinetrack.data.api.TMDBSearchResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MoodSearchRequest(
    val query: String,
    val language: String = "it-IT",
    val page: Int = 1
)

@Serializable
data class MoodSearchResponse(
    val success: Boolean = false,
    @SerialName("vibe_title") val vibeTitle: String? = null,
    @SerialName("vibe_description") val vibeDescription: String? = null,
    val count: Int = 0,
    @SerialName("total_pages") val totalPages: Int = 1,
    val results: List<MoodMovieItemDto> = emptyList(),
    val code: String? = null,
    val message: String? = null
)

@Serializable
data class MoodMovieItemDto(
    val id: Long,
    @SerialName("media_type") val mediaType: String = "movie",
    val title: String,
    val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("genre_ids") val genreIds: List<Long> = emptyList(),
    @SerialName("ai_reason") val aiReason: String? = null
) {
    fun toTMDBSearchResult(): TMDBSearchResult.MovieResult {
        return TMDBSearchResult.MovieResult(
            id = id,
            title = title,
            posterPath = posterPath,
            backdropPath = backdropPath,
            voteAverage = voteAverage,
            releaseDate = releaseDate,
            genreIds = genreIds,
            overview = overview,
            aiReason = aiReason
        )
    }
}

@Serializable
data class MoodHistoryEntry(
    val query: String,
    val timestamp: Long = System.currentTimeMillis(),
    val vibeTitle: String,
    val vibeDescription: String,
    val isAi: Boolean = true,
    val movies: List<TMDBSearchResult.MovieResult> = emptyList()
)
