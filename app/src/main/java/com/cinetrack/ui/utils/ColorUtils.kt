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
     * Extracts the dominant vibrant color from a bitmap.
     * Samples the image and groups pixels into hue buckets, prioritizing vibrant
     * and mid-luminance pixels over dark shadows or highlights.
     */
    suspend fun extractAverageColor(bitmap: Bitmap, defaultFallback: Color = Color.Unspecified): Color = withContext(Dispatchers.Default) {
        if (bitmap.width <= 0 || bitmap.height <= 0) return@withContext defaultFallback

        val scaledWidth = min(48, bitmap.width)
        val scaledHeight = min(48, bitmap.height)
        val smallBitmap = if (bitmap.width == scaledWidth && bitmap.height == scaledHeight) {
            bitmap
        } else {
            Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
        }

        val pixels = IntArray(scaledWidth * scaledHeight)
        smallBitmap.getPixels(pixels, 0, scaledWidth, 0, 0, scaledWidth, scaledHeight)
        if (smallBitmap != bitmap) {
            smallBitmap.recycle()
        }

        val bucketCounts = IntArray(36)
        val bucketRed = LongArray(36)
        val bucketGreen = LongArray(36)
        val bucketBlue = LongArray(36)
        val bucketScore = FloatArray(36)

        val hsv = FloatArray(3)
        var totalColorPixels = 0
        var fallbackR = 0L
        var fallbackG = 0L
        var fallbackB = 0L
        var fallbackCount = 0

        for (pixel in pixels) {
            val alpha = (pixel shr 24) and 0xFF
            if (alpha < 128) continue

            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF

            AndroidColor.colorToHSV(pixel, hsv)
            val hue = hsv[0] // 0..360
            val sat = hsv[1] // 0..1
            val value = hsv[2] // 0..1

            if (value >= 0.15f && value <= 0.92f) {
                fallbackR += r
                fallbackG += g
                fallbackB += b
                fallbackCount++
            }

            if (value < 0.18f || (value > 0.92f && sat < 0.2f) || sat < 0.18f) continue

            val bucketIdx = min(35, max(0, (hue / 10f).toInt()))
            bucketCounts[bucketIdx]++
            bucketRed[bucketIdx] += r.toLong()
            bucketGreen[bucketIdx] += g.toLong()
            bucketBlue[bucketIdx] += b.toLong()

            val lumBonus = if (value in 0.35f..0.85f) 1.5f else 1.0f
            bucketScore[bucketIdx] += sat * lumBonus
            totalColorPixels++
        }

        if (totalColorPixels == 0) {
            if (fallbackCount > 0) {
                return@withContext Color(
                    AndroidColor.rgb(
                        (fallbackR / fallbackCount).toInt().coerceIn(0, 255),
                        (fallbackG / fallbackCount).toInt().coerceIn(0, 255),
                        (fallbackB / fallbackCount).toInt().coerceIn(0, 255)
                    )
                )
            }
            return@withContext defaultFallback
        }

        var bestBucket = 0
        var maxScore = -1f
        for (i in 0 until 36) {
            if (bucketCounts[i] > 0 && bucketScore[i] > maxScore) {
                maxScore = bucketScore[i]
                bestBucket = i
            }
        }

        val count = bucketCounts[bestBucket]
        val avgR = (bucketRed[bestBucket] / count).toInt().coerceIn(0, 255)
        val avgG = (bucketGreen[bestBucket] / count).toInt().coerceIn(0, 255)
        val avgB = (bucketBlue[bestBucket] / count).toInt().coerceIn(0, 255)

        Color(AndroidColor.rgb(avgR, avgG, avgB))
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

    /**
     * Softens an overly bright neon color so it doesn't glare on dark themes,
     * while preserving its vibrant punch without making it too dark.
     */
    fun darkenForAmbient(color: Color, maxBrightness: Float = 0.78f): Color {
        val argb = color.toArgb()
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(argb, hsv)

        // Soften overly intense neon saturation slightly
        hsv[1] = min(0.92f, hsv[1])
        // Cap excessive brightness so it doesn't pierce the eyes, but keep it punchy and vivid
        if (hsv[2] > maxBrightness) {
            hsv[2] = maxBrightness
        }
        return Color(AndroidColor.HSVToColor(hsv))
    }

    /**
     * Aumenta la luminosità di un colore dinamico preservandone la vivacità.
     * Perfetto per testi colorati su sfondi scuri senza l'effetto "pastello slavato".
     */
    fun lightenForText(color: Color, brightnessBoost: Float = 1.35f): Color {
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(color.toArgb(), hsv)

        // hsv[2] è il "Value" (luminosità). Lo moltiplichiamo per il boost, bloccandolo al 100% (1.0f)
        hsv[2] = min(1f, hsv[2] * brightnessBoost)
        
        // Riduciamo impercettibilmente la saturazione (5%) per evitare che i bordi del testo "sbavino" visivamente
        hsv[1] = min(1f, hsv[1] * 0.95f)

        return Color(AndroidColor.HSVToColor(hsv))
    }
}
