package com.cinetrack.util

import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom

object PkceHelper {

    private val secureRandom = SecureRandom()

    /**
     * Generates a cryptographically secure random code_verifier (RFC 7636).
     * 48 bytes encoded with Base64 URL_SAFE without padding yields 64 characters.
     */
    fun generateCodeVerifier(): String {
        val bytes = ByteArray(48)
        secureRandom.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    /**
     * Generates the code_challenge using SHA-256 (S256 method) from the given code_verifier.
     * Output is Base64 URL_SAFE without padding.
     */
    fun generateCodeChallenge(verifier: String): String {
        val bytes = verifier.toByteArray(StandardCharsets.US_ASCII)
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    /**
     * Generates a random state string for CSRF mitigation in OAuth flows.
     */
    fun generateState(): String {
        val bytes = ByteArray(24)
        secureRandom.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }
}
