package com.cinetrack.util

import com.cinetrack.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object SupabaseNotificationService {

    suspend fun notifyUser(
        targetUserId: String,
        title: String? = null,
        body: String? = null,
        titleLocKey: String? = null,
        bodyLocKey: String? = null,
        bodyLocArgs: List<String>? = null,
        mediaId: Long,
        mediaType: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val user = FirebaseAuth.getInstance().currentUser ?: return@withContext false
            val tokenResult = user.getIdToken(false).await()
            val idToken = tokenResult.token ?: return@withContext false

            val functionUrl = "\${BuildConfig.SUPABASE_URL}/functions/v1/notify-user"
            val url = URL(functionUrl)
            val connection = url.openConnection() as HttpURLConnection
            
            connection.requestMethod = "POST"
            connection.setRequestProperty("Authorization", "Bearer \$idToken")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val jsonBody = JSONObject().apply {
                put("targetUserId", targetUserId)
                title?.let { put("title", it) }
                body?.let { put("body", it) }
                titleLocKey?.let { put("titleLocKey", it) }
                bodyLocKey?.let { put("bodyLocKey", it) }
                bodyLocArgs?.let { put("bodyLocArgs", org.json.JSONArray(it)) }
                put("mediaId", mediaId)
                put("mediaType", mediaType)
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(jsonBody.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            return@withContext responseCode in 200..299
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }
}
