package com.cinetrack.data.model

import androidx.annotation.Keep
import com.cinetrack.data.api.TMDBSearchResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class IrisWeeklyCacheDto(
    @SerialName("week_number") val weekNumber: Int,
    @SerialName("year") val year: Int,
    @SerialName("focus_cluster_title") val focusClusterTitle: String,
    @SerialName("focus_subtitle") val focusSubtitle: String,
    @SerialName("used_seed_titles") val usedSeedTitles: List<String>,
    @SerialName("timestamp") val timestamp: Long,
    @SerialName("cluster_offset") val clusterOffset: Int = 0,
    @SerialName("has_multiple_clusters") val hasMultipleClusters: Boolean = false,
    @SerialName("movies") val movies: List<IrisWeeklyMovieDto>
)

@Keep
@Serializable
data class IrisWeeklyMovieDto(
    val id: Long,
    val title: String,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("genre_ids") val genreIds: List<Long> = emptyList(),
    @SerialName("collection_id") val collectionId: Long? = null,
    val overview: String? = null,
    val score: Double? = null,
    @SerialName("ai_reason") val aiReason: String? = null
) {
    fun toMovieResult(): TMDBSearchResult.MovieResult = TMDBSearchResult.MovieResult(
        id = id,
        title = title,
        posterPath = posterPath,
        backdropPath = backdropPath,
        releaseDate = releaseDate,
        voteAverage = voteAverage,
        genreIds = genreIds,
        overview = overview,
        aiReason = aiReason ?: score?.let { "${(it * 100).toInt()}% Sintonia" }
    )

    companion object {
        fun fromMovieResult(result: TMDBSearchResult.MovieResult, collectionId: Long?): IrisWeeklyMovieDto {
            return IrisWeeklyMovieDto(
                id = result.id,
                title = result.title ?: result.displayTitle,
                posterPath = result.posterPath,
                backdropPath = result.backdropPath,
                releaseDate = result.releaseDate,
                voteAverage = result.voteAverage,
                genreIds = result.genreIds,
                collectionId = collectionId,
                overview = result.overview,
                score = null,
                aiReason = result.aiReason
            )
        }
    }
}

data class IrisWeeklyPicksData(
    val weekNumber: Int,
    val year: Int,
    val focusClusterTitle: String,
    val focusSubtitle: String,
    val usedSeedTitles: List<String>,
    val timestamp: Long,
    val clusterOffset: Int = 0,
    val hasMultipleClusters: Boolean = false,
    // Riserva fino a 20 film; la UI applicherà .filter { it.id !in watchedIds }.take(10)
    val candidateMovies: List<TMDBSearchResult.MovieResult>
)

sealed interface IrisWeeklyUiState {
    data object Loading : IrisWeeklyUiState
    data object ColdStartNeeded : IrisWeeklyUiState
    data class Ready(
        val picks: IrisWeeklyPicksData,
        val isRotating: Boolean = false
    ) : IrisWeeklyUiState
    data class Error(val message: String) : IrisWeeklyUiState
}

fun TMDBSearchResult.MovieResult.toMovie(): Movie = Movie(
    id = id,
    title = title ?: "",
    posterPath = posterPath,
    backdropPath = backdropPath,
    voteAverage = voteAverage ?: 0.0,
    releaseDate = releaseDate,
    genreIds = genreIds,
    overview = overview,
    mediaType = "movie"
)
