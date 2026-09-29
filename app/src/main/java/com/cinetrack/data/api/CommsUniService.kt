package com.cinetrack.data.api

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.DELETE
import retrofit2.http.PATCH
import retrofit2.http.Body

interface CommsUniService {

    @GET("v1/health")
    suspend fun checkHealth(): Response<Unit>

    @GET("v1/sources")
    suspend fun getSources(): Response<SourcesResponse>

    @GET("v1/entities/{entityType}/{entityId}/conversation")
    suspend fun getConversationStats(
        @retrofit2.http.Path("entityType") entityType: String,
        @retrofit2.http.Path("entityId") entityId: String,
        @retrofit2.http.Query("source") source: String? = null,
        @retrofit2.http.Query("language") language: String? = null
    ): Response<CommsUniConversationStatsResponse>

    @GET("v1/entities/{entityType}/{entityId}/comments")
    suspend fun getComments(
        @retrofit2.http.Path("entityType") entityType: String,
        @retrofit2.http.Path("entityId") entityId: String,
        @retrofit2.http.Query("sort") sort: String? = null,
        @retrofit2.http.Query("limit") limit: Int? = null,
        @retrofit2.http.Query("cursor") cursor: String? = null,
        @retrofit2.http.Query("include") include: String? = null,
        @retrofit2.http.Query("source") source: String? = null,
        @retrofit2.http.Query("language") language: String? = null
    ): Response<CommsUniCommentsResponse>
    @GET("v1/comments/{commentId}/replies")
    suspend fun getReplies(
        @retrofit2.http.Path("commentId") commentId: String,
        @retrofit2.http.Query("sort") sort: String? = null,
        @retrofit2.http.Query("limit") limit: Int? = null,
        @retrofit2.http.Query("cursor") cursor: String? = null,
        @retrofit2.http.Query("parent") parent: String? = null,
        @retrofit2.http.Query("source") source: String? = null,
        @retrofit2.http.Query("language") language: String? = null
    ): Response<CommsUniCommentsResponse>

    @POST("v1/entities/{entityType}/{entityId}/comments")
    suspend fun createComment(
        @retrofit2.http.Path("entityType") entityType: String,
        @retrofit2.http.Path("entityId") entityId: String,
        @Body request: CommsUniWriteCommentRequest
    ): Response<CommsUniNewCommentResponse>

    @POST("v1/comments/{commentId}/replies")
    suspend fun createReply(
        @retrofit2.http.Path("commentId") commentId: String,
        @Body request: CommsUniWriteCommentRequest
    ): Response<CommsUniNewCommentResponse>

    @PUT("v1/comments/{commentId}/like")
    suspend fun likeComment(
        @retrofit2.http.Path("commentId") commentId: String
    ): Response<Unit>

    @DELETE("v1/comments/{commentId}/like")
    suspend fun unlikeComment(
        @retrofit2.http.Path("commentId") commentId: String
    ): Response<Unit>

    @PATCH("v1/comments/{commentId}")
    suspend fun updateCommentSpoiler(
        @retrofit2.http.Path("commentId") commentId: String,
        @Body request: CommsUniUpdateSpoilerRequest
    ): Response<CommsUniNewCommentResponse>

    @POST("v1/comments/{commentId}/reports")
    suspend fun reportComment(
        @retrofit2.http.Path("commentId") commentId: String,
        @Body request: CommsUniReportRequest
    ): Response<Unit>

    @DELETE("v1/comments/{commentId}")
    suspend fun deleteComment(
        @retrofit2.http.Path("commentId") commentId: String
    ): Response<Unit>

    @PUT("v1/authors/me/profile")
    suspend fun updateProfile(
        @Body request: CommsUniUpdateProfileRequest
    ): Response<CommsUniProfileResponse>
}

@Serializable
data class CommsUniNewCommentResponse(
    val data: CommsUniNewCommentData
)

@Serializable
data class CommsUniNewCommentData(
    val comment: CommsUniComment
)

@Serializable
data class SourcesResponse(
    val data: List<SourceCatalogRow>
)

@Serializable
data class SourceCatalogRow(
    val slug: String,
    val displayName: String? = null,
    val shortName: String? = null,
    val kind: String? = null,
    val accentColor: String? = null,
    val iconUrl: String? = null,
    val status: String? = null
)

@Serializable
data class CommsUniUpdateSpoilerRequest(
    val isSpoiler: Boolean
)
