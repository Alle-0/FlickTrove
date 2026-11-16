package com.cinetrack.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

@Serializable
data class SemanticSearchResponse(
    val results: List<SemanticMovieDto> = emptyList(),
    val error: String? = null
)

@Serializable
data class SemanticMovieDto(
    val id: Long,
    val title: String,
    val year: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    val score: Double? = null
) {
    fun toMovieResult(): TMDBSearchResult.MovieResult {
        val normalizedPoster = posterPath?.trim()?.takeIf { it.isNotBlank() }?.let { path ->
            if (path.startsWith("http") || path.startsWith("/")) path else "/$path"
        }

        return TMDBSearchResult.MovieResult(
            id = id,
            title = title,
            posterPath = normalizedPoster,
            releaseDate = year?.let { if (it.length == 4) "$it-01-01" else it },
            voteAverage = voteAverage,
            overview = null,
            aiReason = score?.let { "Rilevanza semantica: ${(it * 100).toInt()}%" }
        )
    }
}

interface SemanticSearchApi {
    @GET("search")
    suspend fun searchSemantics(
        @Query("q") query: String,
        @Query("limit") limit: Int = 20
    ): Response<SemanticSearchResponse>
}
