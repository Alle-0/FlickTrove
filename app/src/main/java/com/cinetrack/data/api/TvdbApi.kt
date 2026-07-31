package com.cinetrack.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface TvdbApi {
    @POST("v4/login")
    suspend fun login(@Body request: TvdbLoginRequest): TvdbLoginResponse

    @GET("v4/search")
    suspend fun search(
        @Query("query") query: String,
        @Query("year") year: String? = null,
        @Query("type") type: String? = null // "movie" or "series"
    ): TvdbSearchResponse

    @GET("v4/movies/{id}/extended")
    suspend fun getMovieExtended(@Path("id") id: String): TvdbExtendedResponse

    @GET("v4/series/{id}/extended")
    suspend fun getSeriesExtended(
        @Path("id") id: String,
        @Query("meta") meta: String? = null
    ): TvdbExtendedResponse

    @GET("v4/series/{id}/episodes/default")
    suspend fun getSeriesEpisodes(
        @Path("id") id: String,
        @Query("page") page: Int = 0
    ): TvdbEpisodesResponse

    @GET("v4/series/{id}/episodes/absolute")
    suspend fun getSeriesEpisodesAbsolute(
        @Path("id") id: String,
        @Query("page") page: Int = 0
    ): TvdbEpisodesResponse
}
