package com.cinetrack.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.cinetrack.data.api.SupabaseStorageApi
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageRepository @Inject constructor(
    private val api: SupabaseStorageApi,
    private val auth: FirebaseAuth,
    @ApplicationContext private val context: Context
) {
    suspend fun uploadAvatar(imageUri: Uri): Result<String> = runCatching {
        val user = auth.currentUser ?: throw IllegalStateException("User must be logged in to upload an avatar.")
        
        // Retrieve the Firebase ID Token
        val tokenResult = user.getIdToken(false).await()
        val token = tokenResult.token ?: throw IllegalStateException("Could not get Firebase ID token.")
        
        // Compress and resize the image
        val compressedBytes = withContext(Dispatchers.IO) {
            compressImage(imageUri, 512, 512)
        }
        
        val requestBody = compressedBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
        
        val response = api.uploadAvatar(
            authHeader = "Bearer $token",
            imageBytes = requestBody
        )
        
        if (response.publicUrl != null) {
            response.publicUrl
        } else {
            throw Exception(response.error ?: "Unknown error uploading avatar")
        }
    }

    suspend fun uploadCommentImage(imageUri: Uri): Result<String> = runCatching {
        val user = auth.currentUser ?: throw IllegalStateException("User must be logged in to upload an image.")
        
        // Retrieve the Firebase ID Token
        val tokenResult = user.getIdToken(false).await()
        val token = tokenResult.token ?: throw IllegalStateException("Could not get Firebase ID token.")
        
        // Compress and resize the image for comments (larger than avatar, e.g. max 1080x1080)
        val compressedBytes = withContext(Dispatchers.IO) {
            compressImage(imageUri, 1080, 1080)
        }
        
        val requestBody = compressedBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
        
        val response = api.uploadCommentImage(
            authHeader = "Bearer $token",
            imageBytes = requestBody
        )
        
        if (response.publicUrl != null) {
            response.publicUrl
        } else {
            throw Exception(response.error ?: "Unknown error uploading comment image")
        }
    }

    suspend fun deleteCommentImage(imageUrl: String): Result<Boolean> = runCatching {
        val user = auth.currentUser ?: throw IllegalStateException("User must be logged in to delete an image.")
        
        // Retrieve the Firebase ID Token
        val tokenResult = user.getIdToken(false).await()
        val token = tokenResult.token ?: throw IllegalStateException("Could not get Firebase ID token.")
        
        val request = com.cinetrack.data.api.DeleteImageRequest(imageUrl = imageUrl)
        val response = api.deleteCommentImage(
            authHeader = "Bearer $token",
            request = request
        )
        
        if (response.error == null) {
            true
        } else {
            throw Exception(response.error)
        }
    }

    private fun compressImage(uri: Uri, maxWidth: Int, maxHeight: Int): ByteArray {
        val contentResolver = context.contentResolver
        
        // Decode only the bounds first to avoid OOM
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        contentResolver.openInputStream(uri)?.use { 
            BitmapFactory.decodeStream(it, null, options) 
        }

        // Calculate inSampleSize
        var inSampleSize = 1
        if (options.outHeight > maxHeight || options.outWidth > maxWidth) {
            val halfHeight = options.outHeight / 2
            val halfWidth = options.outWidth / 2
            while (halfHeight / inSampleSize >= maxHeight && halfWidth / inSampleSize >= maxWidth) {
                inSampleSize *= 2
            }
        }

        // Decode the actual bitmap
        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
        }
        val bitmap = contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        } ?: throw IllegalStateException("Could not decode image")

        // Compress to JPEG
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        return outputStream.toByteArray()
    }
}
