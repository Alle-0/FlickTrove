package com.cinetrack.data.api

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Url

interface NewsService {
    @GET
    suspend fun getNewsFeed(@Url feedUrl: String): ResponseBody
}
