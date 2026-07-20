package com.cinetrack.util

import androidx.compose.runtime.compositionLocalOf

enum class ImageQuality {
    LOW, MEDIUM, HIGH
}

enum class ImageType {
    POSTER, BACKDROP, PROFILE, LOGO
}

val LocalImageQuality = compositionLocalOf { ImageQuality.MEDIUM }

/**
 * Builds a TMDB image URL based on the specified type and desired quality level.
 *
 * @param path The image path returned by the TMDB API (e.g., "/kqjL17yufvn9OVLyXYpvtyrFfak.jpg").
 * @param type The type of image (Poster, Backdrop, Profile, Logo).
 * @param quality The desired image quality.
 * @return The complete URL, or null if the path is null.
 */
fun buildTmdbImageUrl(path: String?, type: ImageType, quality: ImageQuality): String? {
    if (path.isNullOrBlank()) return null

    val size = when (type) {
        ImageType.POSTER -> when (quality) {
            ImageQuality.LOW -> "w185"
            ImageQuality.MEDIUM -> "w500"
            ImageQuality.HIGH -> "original"
        }
        ImageType.BACKDROP -> when (quality) {
            ImageQuality.LOW -> "w780"
            ImageQuality.MEDIUM -> "w1280"
            ImageQuality.HIGH -> "original"
        }
        ImageType.PROFILE -> when (quality) {
            ImageQuality.LOW -> "w185"
            ImageQuality.MEDIUM -> "h632"
            ImageQuality.HIGH -> "original"
        }
        ImageType.LOGO -> when (quality) {
            ImageQuality.LOW -> "w154"
            ImageQuality.MEDIUM -> "w300"
            ImageQuality.HIGH -> "original"
        }
    }

    // Ensure path starts with a slash
    val safePath = if (path.startsWith("/")) path else "/$path"
    
    return "https://image.tmdb.org/t/p/$size$safePath"
}
