package com.cinetrack.data.model

enum class KlipyMediaType(val apiType: String) {
    GIF("gif"),
    STICKER("sticker")
}

data class GifItem(
    val id: String,
    val slug: String,
    val title: String,
    val previewUrl: String,
    val fullUrl: String,
    val width: Int,
    val height: Int,
    val type: KlipyMediaType = KlipyMediaType.GIF
)
