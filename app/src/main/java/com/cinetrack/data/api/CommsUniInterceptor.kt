package com.cinetrack.data.api

import com.cinetrack.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import okhttp3.Interceptor
import okhttp3.Response
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class CommsUniInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        val builder = originalRequest.newBuilder()
            .header("Authorization", "Bearer ${BuildConfig.COMMSUNI_API_KEY}")
            .header("Accept", "application/json")
            .header("User-Agent", "FlickTrove-Android/${BuildConfig.VERSION_NAME}")

        // 1. Identificazione Utente (Actor ID)
        val user = FirebaseAuth.getInstance().currentUser
        val uid = user?.uid

        if (user != null && !user.isAnonymous && uid != null) {
            val actorId = generateActorId(uid)
            builder.header("X-TVTA-Actor-ID", actorId)
        }

        // 2. Idempotency-Key per scritture POST (richiesto da CommsUni per prevenire duplicati)
        if (originalRequest.method == "POST" && originalRequest.header("Idempotency-Key") == null) {
            builder.header("Idempotency-Key", java.util.UUID.randomUUID().toString())
        }

        return chain.proceed(builder.build())
    }

    companion object {
        fun generateActorId(userId: String): String {
            val secret = BuildConfig.COMMSUNI_HMAC_SECRET
            return try {
                val mac = Mac.getInstance("HmacSHA256")
                val secretKeySpec = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256")
                mac.init(secretKeySpec)
                val hmacBytes = mac.doFinal(userId.toByteArray(Charsets.UTF_8))
                bytesToHex(hmacBytes)
            } catch (e: Exception) {
                // Se fallisce l'HMAC per qualche ragione, usa un fallback deterministico
                val md = MessageDigest.getInstance("SHA-256")
                val hashBytes = md.digest((secret + userId).toByteArray(Charsets.UTF_8))
                bytesToHex(hashBytes)
            }
        }

        private fun bytesToHex(bytes: ByteArray): String {
            val hexChars = CharArray(bytes.size * 2)
            for (i in bytes.indices) {
                val v = bytes[i].toInt() and 0xFF
                hexChars[i * 2] = "0123456789abcdef"[v ushr 4]
                hexChars[i * 2 + 1] = "0123456789abcdef"[v and 0x0F]
            }
            return String(hexChars)
        }
    }
}
