package com.cinetrack.data.model

import kotlinx.serialization.Serializable

@Serializable
data class NewsItem(
    val title: String,
    val link: String,
    val imageUrl: String?,
    val pubDate: String
)

