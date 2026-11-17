package com.cinetrack.data.api

import com.cinetrack.data.model.MoodSearchRequest
import com.cinetrack.data.model.MoodSearchResponse
import com.cinetrack.data.model.SearchLogDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface SupabaseMoodSearchApi {

    @POST("mood-search")
    suspend fun searchMoviesByMood(
        @Body request: MoodSearchRequest
    ): Response<MoodSearchResponse>

    @POST("/rest/v1/search_logs")
    suspend fun recordSearchLog(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body payload: SearchLogDto
    ): Response<Unit>
}
