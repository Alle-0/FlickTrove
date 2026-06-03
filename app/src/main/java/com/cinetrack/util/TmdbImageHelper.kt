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
            ImageQuality.LOW -> "w92"
            ImageQuality.MEDIUM -> "w185" // or w342
            ImageQuality.HIGH -> "w500" // or w780 / original
        }
        ImageType.BACKDROP -> when (quality) {
            ImageQuality.LOW -> "w300"
            ImageQuality.MEDIUM -> "w780"
            ImageQuality.HIGH -> "w1280" // or original
        }
        ImageType.PROFILE -> when (quality) {
            ImageQuality.LOW -> "w45"
            ImageQuality.MEDIUM -> "w185"
            ImageQuality.HIGH -> "h632" // or original
        }
        ImageType.LOGO -> when (quality) {
            ImageQuality.LOW -> "w92"
            ImageQuality.MEDIUM -> "w154"
            ImageQuality.HIGH -> "w300" // or w500
        }
    }

    // Ensure path starts with a slash
    val safePath = if (path.startsWith("/")) path else "/$path"
    
    return "https://image.tmdb.org/t/p/$size$safePath"
}
