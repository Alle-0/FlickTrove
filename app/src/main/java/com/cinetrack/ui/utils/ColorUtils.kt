package com.cinetrack.ui.utils

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import android.graphics.Color as AndroidColor
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.max

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ColorUtils {
    /**
     * Extracts the dominant ambient color from a bitmap using Android's Palette API.
     *
     * @param bitmap       The source bitmap.
     * @param useBottomHalf If true, only the bottom 50% of the image is sampled.
     * @param targetAspectRatio If provided, computes the visible area assuming ContentScale.Crop
     *                          and Center alignment, ignoring cropped-out edges.
     * @param fallback     Returned when extraction fails.
     */
    suspend fun extractAccentColor(
        bitmap: Bitmap,
        useBottomHalf: Boolean = false,
        targetAspectRatio: Float? = null,
        fallback: Color = Color.Unspecified
    ): Color = withContext(Dispatchers.Default) {
        if (bitmap.width <= 0 || bitmap.height <= 0) return@withContext fallback

        val builder = androidx.palette.graphics.Palette.Builder(bitmap)

        if (useBottomHalf) {
            builder.setRegion(0, bitmap.height / 2, bitmap.width, bitmap.height)
        } else if (targetAspectRatio != null && targetAspectRatio > 0) {
            val imageAspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
            val visibleWidth: Int
            val visibleHeight: Int

            if (targetAspectRatio > imageAspectRatio) {
                // Target is wider, so height is cropped
                visibleWidth = bitmap.width
                visibleHeight = (bitmap.width / targetAspectRatio).toInt().coerceAtMost(bitmap.height)
            } else {
                // Target is taller, so width is cropped
                visibleHeight = bitmap.height
                visibleWidth = (bitmap.height * targetAspectRatio).toInt().coerceAtMost(bitmap.width)
            }

            val left = (bitmap.width - visibleWidth) / 2
            val top = (bitmap.height - visibleHeight) / 2
            builder.setRegion(left, top, left + visibleWidth, top + visibleHeight)
        }

        val palette = builder.generate()
        val colorInt = palette.dominantSwatch?.rgb
            ?: palette.mutedSwatch?.rgb
            ?: palette.vibrantSwatch?.rgb
            ?: return@withContext fallback

        val raw = Color(colorInt)
        val ambient = darkenForAmbient(raw)
        ensureMinimumLuminance(ambient, 0.25f)
    }

    /**
     * @deprecated Use [extractAccentColor] instead.
     * Kept for backwards-compatibility with call sites not yet migrated.
     */
    @Deprecated(
        message = "Use extractAccentColor() which uses Palette and samples the bottom half of the image for a better ambient color.",
        replaceWith = ReplaceWith("extractAccentColor(bitmap)")
    )
    suspend fun extractAverageColor(bitmap: Bitmap, defaultFallback: Color = Color.Unspecified): Color = withContext(Dispatchers.Default) {
        if (bitmap.width <= 0 || bitmap.height <= 0) return@withContext defaultFallback
        val palette = androidx.palette.graphics.Palette.Builder(bitmap).generate()
        val colorInt = palette.dominantSwatch?.rgb
            ?: palette.mutedSwatch?.rgb
            ?: palette.vibrantSwatch?.rgb
        if (colorInt != null) Color(colorInt) else defaultFallback
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
     * Garantisce che il colore di accento estratto sia sempre sufficientemente luminoso e vivido
     * per pulsanti, icone, pillole e badge su tema scuro, evitando che appaia troppo scuro o spento.
     */
    fun ensureVividAccent(color: Color, minBrightness: Float = 0.88f, minSaturation: Float = 0.45f): Color {
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(color.toArgb(), hsv)

        if (hsv[1] > 0.05f && hsv[1] < minSaturation) {
            hsv[1] = minSaturation
        }
        if (hsv[2] < minBrightness) {
            hsv[2] = minBrightness
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

    /**
     * Calcola il colore del testo/icone di contrasto per uno sfondo [color].
     * Se la luminanza del colore di accento è alta (es. Teal, Giallo, Rosa chiaro), restituisce DeepBlack.
     * Se è bassa o media (es. Blu, Viola, Rosso, Verde scuro), restituisce Color.White per garantire la massima visibilità.
     */
    fun contentColorForAccent(color: Color): Color {
        return if (color.luminance() > 0.38f) Color(0xFF000000) else Color.White
    }
}

/** Converts a Compose [Color] to a CSS-style hex string (e.g. "#FF3A2C"). */
fun Color.toHexString(): String {
    val r = (red * 255).toInt().coerceIn(0, 255)
    val g = (green * 255).toInt().coerceIn(0, 255)
    val b = (blue * 255).toInt().coerceIn(0, 255)
    return String.format("#%02X%02X%02X", r, g, b)
}
