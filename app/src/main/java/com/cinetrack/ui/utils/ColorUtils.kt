package com.cinetrack.ui.utils

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import android.graphics.Color as AndroidColor
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.max

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ColorUtils {
    /**
     * Extracts the average color from a bitmap by scaling it down to 1x1.
     * Matches the logic in the TS version (Skia 1x1 surface).
     */
    suspend fun extractAverageColor(bitmap: Bitmap): Color = withContext(Dispatchers.Default) {
        if (bitmap.width <= 0 || bitmap.height <= 0) return@withContext Color.Transparent
        val smallBitmap = Bitmap.createScaledBitmap(bitmap, 1, 1, true)
        val colorInt = smallBitmap.getPixel(0, 0)
        if (smallBitmap != bitmap) {
            smallBitmap.recycle()
        }
        Color(colorInt)
    }

    /**
     * Ensures that a color meets a minimum luminance threshold.
     * If the color is too dark, it's brightened.
     * Replicates ensureMinimumLuminance from colorExtractor.ts
     */
    fun ensureMinimumLuminance(color: Color, threshold: Float = 0.3f): Color {
        val argb = color.toArgb()
        var r = (argb shr 16) and 0xFF
        var g = (argb shr 8) and 0xFF
        var b = argb and 0xFF

        // Perceived luminance formula
        val luminance = (0.299f * r + 0.587f * g + 0.114f * b) / 255f

        if (luminance < threshold) {
            // Handling pure black or extremely dark colors
            if (r == 0 && g == 0 && b == 0) {
                return Color(0xFF71717A) // A lighter neutral gray (zinc-400)
            }

            val factor = threshold / (if (luminance == 0f) 0.05f else luminance)
            r = min(255, floor(r * factor).toInt())
            g = min(255, floor(g * factor).toInt())
            b = min(255, floor(b * factor).toInt())

            // Final check for very dark results: boost again
            val newLuminance = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
            if (newLuminance < threshold * 1.5f) {
                val boost = (threshold * 1.5f) / (if (newLuminance == 0f) 0.1f else newLuminance)
                r = min(255, floor(r * boost).toInt())
                g = min(255, floor(g * boost).toInt())
                b = min(255, floor(b * boost).toInt())
            }
        }

        return Color(AndroidColor.rgb(r, g, b))
    }

    /**
     * Increases the saturation of a color to make it more "vivid".
     */
    fun saturateColor(color: Color, factor: Float = 1.2f): Color {
        val argb = color.toArgb()
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(argb, hsv)
        hsv[1] = min(1f, hsv[1] * factor)
        return Color(AndroidColor.HSVToColor(hsv))
    }
}
