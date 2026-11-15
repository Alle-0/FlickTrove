package com.cinetrack.data.api

import com.cinetrack.data.model.MoodSearchRequest
import com.cinetrack.data.model.MoodSearchResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface SupabaseMoodSearchApi {

    @POST("mood-search")
    suspend fun searchMoviesByMood(
        @Body request: MoodSearchRequest
    ): Response<MoodSearchResponse>
}
