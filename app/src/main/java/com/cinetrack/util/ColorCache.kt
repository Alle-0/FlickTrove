package com.cinetrack.util

import androidx.compose.ui.graphics.Color
import java.util.concurrent.ConcurrentHashMap

object ColorCache {
    private val cache = ConcurrentHashMap<String, Color>()

    fun parseColor(hex: String?, defaultColor: Color = Color.White): Color {
        if (hex.isNullOrBlank()) return defaultColor
        return cache.getOrPut(hex) {
            try {
                val formattedHex = if (!hex.startsWith("#")) {
                    "#$hex"
                } else {
                    hex
                }
                Color(android.graphics.Color.parseColor(formattedHex))
            } catch (e: Exception) {
                defaultColor
            }
        }
    }
}

fun String?.toComposeColor(defaultColor: Color = Color.White): Color {
    return ColorCache.parseColor(this, defaultColor)
}

fun String?.toComposeColorOrNull(): Color? {
    if (this.isNullOrBlank()) return null
    val parsed = ColorCache.parseColor(this, Color.Unspecified)
    return if (parsed == Color.Unspecified) null else parsed
}
