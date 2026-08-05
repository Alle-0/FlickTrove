package com.cinetrack.data.api

import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

import kotlinx.serialization.Serializable

@Serializable
data class UploadResponse(
    val publicUrl: String? = null,
    val error: String? = null
)

interface SupabaseStorageApi {
    
    @POST("upload-avatar")
    suspend fun uploadAvatar(
        @Header("Authorization") authHeader: String,
        @Body imageBytes: RequestBody
    ): UploadResponse
}
