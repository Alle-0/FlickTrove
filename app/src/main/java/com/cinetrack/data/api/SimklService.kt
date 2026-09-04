package com.cinetrack.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface SimklService {

    @POST("oauth/token")
    suspend fun getAccessToken(
        @Body request: SimklTokenRequest
    ): SimklTokenResponse

    @GET("sync/activities")
    suspend fun getActivities(): SimklActivitiesResponse

    @POST("sync/history")
    suspend fun addToHistory(
        @Body request: SimklSyncHistoryRequest
    ): SimklSyncResponse

    @POST("sync/history/remove")
    suspend fun removeFromHistory(
        @Body request: SimklSyncHistoryRequest
    ): SimklSyncResponse

    @POST("sync/watchlist")
    suspend fun addToWatchlist(
        @Body request: SimklSyncWatchlistRequest
    ): SimklSyncResponse

    @POST("sync/watchlist/remove")
    suspend fun removeFromWatchlist(
        @Body request: SimklSyncWatchlistRequest
    ): SimklSyncResponse

    @POST("sync/add-to-list")
    suspend fun addToList(
        @Body request: SimklAddToListRequest
    ): SimklSyncResponse
    
    @GET("sync/movies")
    suspend fun getSyncMovies(): List<SimklSyncItemResponse>
    
    @GET("sync/shows")
    suspend fun getSyncShows(): List<SimklSyncItemResponse>

    @GET("sync/anime")
    suspend fun getSyncAnime(): List<SimklSyncItemResponse>
    
    @GET("sync/all-items/")
    suspend fun getSyncAllItems(
        @Query("date_from") dateFrom: String
    ): SimklAllItemsResponse
}
