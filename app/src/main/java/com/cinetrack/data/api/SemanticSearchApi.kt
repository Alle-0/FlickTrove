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
data class IrisWeeklyResponse(
    val results: List<SemanticMovieDto> = emptyList(),
    @SerialName("used_seeds") val usedSeeds: List<Long> = emptyList(),
    val error: String? = null
)

@Serializable
data class SemanticMovieDto(
    val id: Long,
    val title: String,
    @SerialName("title_it") val titleIt: String? = null,
    @SerialName("original_title") val originalTitle: String? = null,
    val year: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("imdb_id") val imdbId: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("vote_count") val voteCount: Int? = null,
    val popularity: Double? = null,
    @SerialName("genre_ids") val genreIds: List<Long>? = null,
    val genres: String? = null,
    val score: Double? = null,
    @SerialName("raw_similarity") val rawSimilarity: Double? = null,
    @SerialName("ai_reason") val aiReason: String? = null,
    val director: String? = null,
    val writer: String? = null,
    val cast: String? = null,
    val producers: String? = null,
    val composer: String? = null,
    val cinematographer: String? = null,
    val studio: String? = null,
    @SerialName("collection_id") val collectionId: Long? = null,
    val franchise: String? = null,
    val runtime: Int? = null,
    val revenue: Long? = null,
    @SerialName("original_language") val originalLanguage: String? = null,
    val country: String? = null,
    val tagline: String? = null,
    val keywords: String? = null,
    val overview: String? = null
) {
    fun toMovieResult(customReason: String? = null, isItalian: Boolean = false): TMDBSearchResult.MovieResult {
        val normalizedPoster = posterPath?.trim()?.takeIf { it.isNotBlank() }?.let { path ->
            if (path.startsWith("http") || path.startsWith("/")) path else "/$path"
        }
        val normalizedBackdrop = backdropPath?.trim()?.takeIf { it.isNotBlank() }?.let { path ->
            if (path.startsWith("http") || path.startsWith("/")) path else "/$path"
        }

        // Risoluzione Titolo localizzato con fallback sicuro:
        // - Se l'utente usa l'app in italiano, mostra il titolo italiano (se presente), altrimenti fallback sul titolo internazionale.
        // - Se l'utente usa l'app in inglese o altra lingua, mostra sempre il titolo internazionale in inglese.
        // - Garanzia anti-vuoto: fallback progressivo su title -> originalTitle -> "Unknown".
        val finalTitle = if (isItalian) {
            titleIt?.trim()?.takeIf { it.isNotBlank() } ?: title.trim().ifBlank { originalTitle?.trim().orEmpty() }
        } else {
            title.trim().ifBlank { originalTitle?.trim().orEmpty() }
        }.ifBlank { "Unknown" }

        // Data di rilascio precisa o fallback sull'anno
        val finalReleaseDate = releaseDate?.trim()?.takeIf { it.isNotBlank() }
            ?: year?.let { if (it.length == 4) "$it-01-01" else it }

        return TMDBSearchResult.MovieResult(
            id = id,
            title = finalTitle,
            posterPath = normalizedPoster,
            backdropPath = normalizedBackdrop,
            releaseDate = finalReleaseDate,
            voteAverage = voteAverage,
            genreIds = genreIds ?: emptyList(),
            overview = overview,
            aiReason = customReason ?: aiReason ?: score?.let { "Rilevanza semantica: ${(it * 100).toInt()}%" }
        )
    }
}

interface SemanticSearchApi {
    @GET("search")
    suspend fun searchSemantics(
        @Query("q") query: String,
        @Query("lang") lang: String = "en",
        @Query("q_en") queryEn: String? = null,
        @Query("limit") limit: Int = 100
    ): Response<SemanticSearchResponse>

    @GET("iris-weekly")
    suspend fun getIrisWeeklyPicks(
        @Query("seeds") seeds: String,
        @Query("lang") lang: String = "it"
    ): Response<IrisWeeklyResponse>
}
