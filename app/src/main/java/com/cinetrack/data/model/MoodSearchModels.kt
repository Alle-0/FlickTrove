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
    val vibeTitle: String = "Atmosfera Cinematografica",
    val vibeDescription: String = "",
    val isAi: Boolean = true,
    val movies: List<TMDBSearchResult.MovieResult> = emptyList(),
    val aiVibeTitle: String? = null,
    val aiVibeDescription: String? = null,
    val aiMovies: List<TMDBSearchResult.MovieResult>? = null,
    val algoVibeTitle: String? = null,
    val algoVibeDescription: String? = null,
    val algoMovies: List<TMDBSearchResult.MovieResult>? = null
) {
    fun hasAi(): Boolean = !aiMovies.isNullOrEmpty() || (isAi && movies.isNotEmpty())
    fun hasAlgo(): Boolean = !algoMovies.isNullOrEmpty() || (!isAi && movies.isNotEmpty())

    fun resolveAiResults(): List<TMDBSearchResult.MovieResult> =
        aiMovies ?: if (isAi) movies else emptyList()

    fun resolveAlgoResults(): List<TMDBSearchResult.MovieResult> =
        algoMovies ?: if (!isAi) movies else emptyList()

    fun resolveAiVibeTitle(): String =
        aiVibeTitle ?: if (isAi) vibeTitle else "Atmosfera Cinematografica"

    fun resolveAiVibeDescription(): String =
        aiVibeDescription ?: if (isAi) vibeDescription else ""

    fun resolveAlgoVibeTitle(): String =
        algoVibeTitle ?: if (!isAi) vibeTitle else "Atmosfera Cinematografica"

    fun resolveAlgoVibeDescription(): String =
        algoVibeDescription ?: if (!isAi) vibeDescription else ""
}
