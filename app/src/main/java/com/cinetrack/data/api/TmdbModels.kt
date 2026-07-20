package com.cinetrack.data.api

import androidx.compose.runtime.Stable
import com.cinetrack.data.model.Movie
import com.cinetrack.data.model.Genre
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
data class SearchResponse(
    val results: List<Movie>,
    val page: Int? = 1,
    @SerialName("total_pages") val totalPages: Int? = 1,
    @SerialName("total_results") val totalResults: Int? = 0
)

@Serializable
data class MultiSearchResponse(
    val results: List<TMDBSearchResult>,
    val page: Int? = 1,
    @SerialName("total_pages") val totalPages: Int? = 1,
    @SerialName("total_results") val totalResults: Int? = 0
)

@Serializable
data class CollectionSearchResponse(
    val results: List<TMDBSearchResult.CollectionResult>,
    val page: Int? = 1,
    @SerialName("total_pages") val totalPages: Int? = 1,
    @SerialName("total_results") val totalResults: Int? = 0
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("media_type")
@Stable
sealed class TMDBSearchResult {
    abstract val id: Long
    abstract val mediaType: String
    abstract val displayTitle: String

    @Serializable
    @SerialName("movie")
    data class MovieResult(
        override val id: Long,
        val title: String? = null,
        @SerialName("poster_path") val posterPath: String? = null,
        @SerialName("backdrop_path") val backdropPath: String? = null,
        @SerialName("vote_average") val voteAverage: Double? = null,
        @SerialName("release_date") val releaseDate: String? = null,
        @SerialName("genre_ids") val genreIds: List<Long> = emptyList(),
        val overview: String? = null
    ) : TMDBSearchResult() {
        override val mediaType: String get() = "movie"
        override val displayTitle: String get() = title ?: ""
    }

    @Serializable
    @SerialName("tv")
    data class TvResult(
        override val id: Long,
        val name: String? = null,
        @SerialName("poster_path") val posterPath: String? = null,
        @SerialName("backdrop_path") val backdropPath: String? = null,
        @SerialName("vote_average") val voteAverage: Double? = null,
        @SerialName("first_air_date") val firstAirDate: String? = null,
        @SerialName("genre_ids") val genreIds: List<Long> = emptyList(),
        val overview: String? = null
    ) : TMDBSearchResult() {
        override val mediaType: String get() = "tv"
        override val displayTitle: String get() = name ?: ""
    }

    @Serializable
    @SerialName("person")
    data class PersonResult(
        override val id: Long,
        val name: String,
        @SerialName("profile_path") val profilePath: String? = null,
        @SerialName("known_for_department") val knownForDepartment: String? = null,
        @SerialName("known_for") val knownFor: List<Movie> = emptyList()
    ) : TMDBSearchResult() {
        override val mediaType: String get() = "person"
        override val displayTitle: String get() = name
    }

    @Serializable
    @SerialName("collection")
    data class CollectionResult(
        override val id: Long,
        val name: String? = null,
        @SerialName("poster_path") val posterPath: String? = null,
        @SerialName("backdrop_path") val backdropPath: String? = null,
        val overview: String? = null,
        val partsPosterPaths: List<String> = emptyList(),
        val partsCount: Int = 0
    ) : TMDBSearchResult() {
        override val mediaType: String get() = "collection"
        override val displayTitle: String get() = name ?: ""
    }
}

@Serializable
data class Person(
    val id: Long,
    val name: String,
    @SerialName("profile_path") val profilePath: String? = null,
    @SerialName("known_for_department") val knownForDepartment: String? = null,
    val biography: String? = null,
    val birthday: String? = null,
    @SerialName("place_of_birth") val placeOfBirth: String? = null,
    val popularity: Double? = null,
    @SerialName("accent_color") val accentColor: String? = null,
    @SerialName("combined_credits") val combinedCredits: CombinedCredits? = null
)

@Serializable
data class CombinedCredits(
    val cast: List<Movie>,
    val crew: List<Movie>
)

@Serializable
data class PersonSearchResponse(
    val page: Int,
    val results: List<PersonSearchResult>,
    @SerialName("total_pages") val totalPages: Int,
    @SerialName("total_results") val totalResults: Int
)

@Serializable
data class PersonSearchResult(
    val id: Long,
    val name: String,
    @SerialName("profile_path") val profilePath: String?,
    @SerialName("known_for_department") val knownForDepartment: String?,
    @SerialName("media_type") val mediaType: String = "person"
)

@Serializable
data class MovieDetailResponse(
    val id: Long,
    val title: String? = null,
    val name: String? = null,
    @SerialName("original_title") val originalTitle: String? = null,
    @SerialName("original_name") val originalName: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("vote_count") val voteCount: Int? = 0,
    val overview: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    val runtime: Int? = 0,
    @SerialName("episode_run_time") val episodeRunTime: List<Int>? = null,
    val genres: List<Genre>? = null,
    val credits: CreditsResponse? = null,
    val videos: VideoResponse? = null,
    val recommendations: SearchResponse? = null,
    @SerialName("external_ids") val externalIds: ExternalIds? = null,
    @SerialName("watch/providers") val watchProviders: WatchProvidersResponse? = null,
    @SerialName("belongs_to_collection") val belongsToCollection: Collection? = null,
    val status: String? = null,
    @SerialName("next_episode_to_air") val nextEpisodeToAir: com.cinetrack.data.model.Episode? = null,
    @SerialName("last_episode_to_air") val lastEpisodeToAir: com.cinetrack.data.model.Episode? = null,
    @SerialName("last_air_date") val lastAirDate: String? = null,
    val tagline: String? = null,
    val budget: Long? = 0,
    val revenue: Long? = 0,
    @SerialName("original_language") val originalLanguage: String? = null,
    @SerialName("production_companies") val productionCompanies: List<ProductionCompany>? = null,
    @SerialName("number_of_seasons") val numberOfSeasons: Int? = 0,
    @SerialName("number_of_episodes") val numberOfEpisodes: Int? = 0,
    val seasons: List<com.cinetrack.data.model.Season>? = null,
    @SerialName("release_dates") val releaseDates: ReleaseDatesResponse? = null,
    @SerialName("content_ratings") val contentRatings: ContentRatingsResponse? = null,
    @SerialName("production_countries") val productionCountries: List<ProductionCountry>? = null,
    val keywords: KeywordsResponse? = null,
    val images: ImagesResponse? = null
)

@Serializable
data class ReleaseDatesResponse(
    val results: List<ReleaseDateResult>? = null
)

@Serializable
data class ReleaseDateResult(
    @SerialName("iso_3166_1") val iso31661: String = "",
    @SerialName("release_dates") val releaseDates: List<ReleaseDateItem> = emptyList()
)

@Serializable
data class ReleaseDateItem(
    val certification: String = "",
    val type: Int? = null,
    @SerialName("release_date") val releaseDate: String? = null
)

@Serializable
data class ContentRatingsResponse(
    val results: List<ContentRatingResult>? = null
)

@Serializable
data class ContentRatingResult(
    @SerialName("iso_3166_1") val iso31661: String = "",
    val rating: String = ""
)

@Serializable
data class ProductionCompany(
    val id: Long = 0L,
    val name: String = "",
    @SerialName("logo_path") val logoPath: String? = null,
    @SerialName("origin_country") val originCountry: String? = null
)

@Serializable
data class ProductionCountry(
    @SerialName("iso_3166_1") val iso31661: String = "",
    val name: String = ""
)

@Serializable
@Stable
data class Provider(
    @SerialName("provider_id") val providerId: Long = 0L,
    @SerialName("provider_name") val providerName: String = "",
    @SerialName("logo_path") val logoPath: String? = null
)

@Serializable
data class WatchProviderResult(
    val link: String? = null,
    val flatrate: List<Provider>? = null,
    val rent: List<Provider>? = null,
    val buy: List<Provider>? = null
)

@Serializable
data class WatchProvidersResponse(
    val results: Map<String, WatchProviderResult>? = null
)

@Serializable
data class Collection(
    val id: Long = 0L,
    val name: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null
)

@Serializable
data class CollectionResponse(
    val id: Long = 0L,
    val name: String = "",
    val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    val parts: List<Movie> = emptyList()
)



@Serializable
data class CreditsResponse(
    val cast: List<CastMember> = emptyList(),
    val crew: List<CrewMember> = emptyList()
)

@Serializable
@Stable
data class CastMember(
    val id: Long = 0L,
    val name: String = "",
    val character: String? = null,
    @SerialName("profile_path") val profilePath: String? = null
)

@Serializable
@Stable
data class CrewMember(
    val id: Long = 0L,
    val name: String = "",
    val job: String = "",
    @SerialName("profile_path") val profilePath: String? = null
)

@Serializable
data class VideoResponse(
    val results: List<Video> = emptyList()
)

@Serializable
@Stable
data class Video(
    val id: String = "",
    val key: String = "",
    val name: String = "",
    val site: String = "",
    val type: String = ""
)

@Serializable
data class ExternalIds(
    @SerialName("imdb_id") val imdbId: String? = null,
    @SerialName("tvdb_id") val tvdbId: Int? = null,
)

@Serializable
data class KeywordsResponse(
    val keywords: List<Keyword>? = null,
    val results: List<Keyword>? = null
) {
    fun getList(): List<Keyword> = keywords ?: results ?: emptyList()
}

@Serializable
@Stable
data class Keyword(
    val id: Long,
    val name: String
)

@Serializable
data class ImageItem(
    @SerialName("file_path") val filePath: String,
    @SerialName("iso_639_1") val iso6391: String? = null
)

@Serializable
data class ImagesResponse(
    val logos: List<ImageItem>? = null,
    val backdrops: List<ImageItem>? = null,
    val posters: List<ImageItem>? = null
)
