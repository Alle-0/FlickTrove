package com.cinetrack.data.api

import com.cinetrack.data.models.Season
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TMDBService {
    @GET("movie/{id}")
    suspend fun getMovieDetails(
        @Path("id") id: Long,

        @Query(value = "append_to_response", encoded = true) appendToResponse: String = "credits,videos,recommendations,external_ids,watch/providers,release_dates,keywords,images",
        @Query(value = "include_image_language", encoded = true) includeImageLanguage: String = "it,en,null"
    ): MovieDetailResponse

    @GET("tv/{id}")
    suspend fun getTVDetails(
        @Path("id") id: Long,

        @Query(value = "append_to_response", encoded = true) appendToResponse: String = "credits,videos,recommendations,external_ids,watch/providers,content_ratings,keywords,images",
        @Query(value = "include_image_language", encoded = true) includeImageLanguage: String = "it,en,null"
    ): MovieDetailResponse

    // Lightweight fetch for Trakt Sync
    @GET("movie/{id}")
    suspend fun getMovieBasicDetails(
        @Path("id") id: Long,
        @Query(value = "include_image_language", encoded = true) includeImageLanguage: String = "it,en,null"
    ): MovieDetailResponse

    @GET("tv/{id}")
    suspend fun getTVBasicDetails(
        @Path("id") id: Long,
        @Query(value = "include_image_language", encoded = true) includeImageLanguage: String = "it,en,null"
    ): MovieDetailResponse

    @GET("movie/{id}/reviews")
    suspend fun getMovieReviews(
        @Path("id") id: Long,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): ReviewsResponse

    @GET("tv/{id}/reviews")
    suspend fun getTVReviews(
        @Path("id") id: Long,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): ReviewsResponse

    @GET("movie/{id}/recommendations")
    suspend fun getMovieRecommendations(
        @Path("id") id: Long,

        @Query("page") page: Int = 1
    ): SearchResponse

    @GET("tv/{id}/recommendations")
    suspend fun getTVRecommendations(
        @Path("id") id: Long,

        @Query("page") page: Int = 1
    ): SearchResponse

    @GET("search/movie")
    suspend fun searchMovie(
        @Query("query") query: String,

        @Query("page") page: Int = 1,
        @Query("year") year: String? = null
    ): SearchResponse

    @GET("search/tv")
    suspend fun searchTV(
        @Query("query") query: String,

        @Query("page") page: Int = 1
    ): SearchResponse

    @GET("search/multi")
    suspend fun searchMulti(
        @Query("query") query: String,

        @Query("page") page: Int = 1
    ): MultiSearchResponse

    @GET("person/{id}?append_to_response=combined_credits")
    suspend fun getPersonDetails(
        @Path("id") id: Long
    ): Person

    @GET("search/person")
    suspend fun searchPeople(
        @Query("query") query: String,

        @Query("page") page: Int = 1
    ): PersonSearchResponse

    @GET("search/keyword")
    suspend fun searchKeyword(
        @Query("query") query: String,
        @Query("page") page: Int = 1
    ): KeywordsResponse

    @GET("discover/movie")
    suspend fun getMoviesByGenre(
        @Query("with_genres") genreId: Long,

        @Query("page") page: Int = 1
    ): SearchResponse

    @GET("discover/tv")
    suspend fun getTVShowsByGenre(
        @Query("with_genres") genreId: Long,

        @Query("page") page: Int = 1
    ): SearchResponse

    @GET("discover/movie")
    suspend fun discoverMovies(

        @Query("page") page: Int = 1,
        @retrofit2.http.QueryMap options: Map<String, String> = emptyMap()
    ): SearchResponse

    @GET("discover/tv")
    suspend fun discoverTV(

        @Query("page") page: Int = 1,
        @retrofit2.http.QueryMap options: Map<String, String> = emptyMap()
    ): SearchResponse

    @GET("movie/popular")
    suspend fun getPopularMovies(

        @Query("page") page: Int = 1
    ): SearchResponse

    @GET("movie/now_playing")
    suspend fun getNowPlayingMovies(

        @Query("page") page: Int = 1,
        @Query("region") region: String = "IT"
    ): SearchResponse

    @GET("movie/upcoming")
    suspend fun getUpcomingMovies(

        @Query("page") page: Int = 1,
        @Query("region") region: String = "IT"
    ): SearchResponse

    @GET("tv/popular")
    suspend fun getPopularTV(

        @Query("page") page: Int = 1
    ): SearchResponse

    @GET("tv/airing_today")
    suspend fun getAiringTodayTV(

        @Query("page") page: Int = 1
    ): SearchResponse

    @GET("tv/on_the_air")
    suspend fun getOnTheAirTV(

        @Query("page") page: Int = 1
    ): SearchResponse

    @GET("trending/all/week")
    suspend fun getTrendingAll(

        @Query("page") page: Int = 1
    ): SearchResponse

    @GET("trending/movie/week")
    suspend fun getTrendingMovies(

        @Query("page") page: Int = 1
    ): SearchResponse

    @GET("trending/tv/week")
    suspend fun getTrendingTV(

        @Query("page") page: Int = 1
    ): SearchResponse

    @GET("trending/person/week")
    suspend fun getTrendingPeople(

        @Query("page") page: Int = 1
    ): PersonSearchResponse

    @GET("person/popular")
    suspend fun getPopularPeople(

        @Query("page") page: Int = 1
    ): PersonSearchResponse

    @GET("tv/{id}/season/{season_number}")
    suspend fun getSeasonDetails(
        @Path("id") id: Long,
        @Path("season_number") seasonNumber: Int,

    ): Season

    @GET("collection/{id}")
    suspend fun getCollectionDetails(
        @Path("id") id: Long,

    ): CollectionResponse

    @GET("find/{external_id}")
    suspend fun findByExternalId(
        @Path("external_id") externalId: String,
        @Query("external_source") externalSource: String,

    ): FindResponse
}
