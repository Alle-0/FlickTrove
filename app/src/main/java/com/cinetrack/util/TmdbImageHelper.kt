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
    if (path.startsWith("http")) return path

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

/**
 * A Coil transformation that automatically detects dark/monochrome logos (e.g. black TMDB studio logos
 * on transparent backgrounds like HBO, Miramax, Columbia Pictures, Apple, Legendary, etc.)
 * and converts their dark pixels into crisp white, while leaving colored logos
 * (like Marvel, Netflix, Warner Bros, Universal) completely untouched.
 */
class WhiteLogoTransformation : coil.transform.Transformation {
    override val cacheKey: String = "WhiteLogoTransformation_v1"

    override suspend fun transform(input: android.graphics.Bitmap, size: coil.size.Size): android.graphics.Bitmap {
        val width = input.width
        val height = input.height
        if (width <= 0 || height <= 0) return input

        val pixels = IntArray(width * height)
        input.getPixels(pixels, 0, width, 0, 0, width, height)

        var visiblePixels = 0
        var darkPixels = 0
        var coloredPixels = 0

        // Step 1: Analyze non-transparent pixels
        for (pixel in pixels) {
            val a = (pixel ushr 24) and 0xFF
            if (a > 30) {
                visiblePixels++
                val r = (pixel ushr 16) and 0xFF
                val g = (pixel ushr 8) and 0xFF
                val b = pixel and 0xFF

                val maxC = maxOf(r, g, b)
                val minC = minOf(r, g, b)
                if ((maxC - minC) > 30) {
                    coloredPixels++
                }

                val lum = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                if (lum < 110) {
                    darkPixels++
                }
            }
        }

        if (visiblePixels < 10) return input

        val isColored = (coloredPixels.toFloat() / visiblePixels) > 0.08f
        val isMostlyDark = (darkPixels.toFloat() / visiblePixels) > 0.40f

        // If it's a colored logo (e.g. Marvel, Netflix, WB, Universal) or already light, keep original!
        if (isColored || !isMostlyDark) {
            return input
        }

        // Step 2: Monochrome dark logo -> Convert to white preserving alpha anti-aliasing
        val output = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        val outPixels = IntArray(width * height)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val a = (pixel ushr 24) and 0xFF
            if (a > 0) {
                val r = (pixel ushr 16) and 0xFF
                val g = (pixel ushr 8) and 0xFF
                val b = pixel and 0xFF
                val lum = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                if (lum < 130) {
                    // Turn dark pixel into pure white, maintaining original alpha anti-aliasing
                    outPixels[i] = (a shl 24) or 0x00FFFFFF
                } else {
                    outPixels[i] = pixel
                }
            } else {
                outPixels[i] = 0
            }
        }

        output.setPixels(outPixels, 0, width, 0, 0, width, height)
        return output
    }
}

