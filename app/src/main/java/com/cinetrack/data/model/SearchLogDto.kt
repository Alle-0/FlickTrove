package com.cinetrack.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchLogDto(
    @SerialName("query") val query: String,
    @SerialName("engine") val engine: String,
    @SerialName("tier") val tier: String,
    @SerialName("language") val language: String,
    @SerialName("results_count") val resultsCount: Int,
    @SerialName("results_sample") val resultsSample: List<SearchLogMovieSampleDto>? = null,
    @SerialName("vibe_title") val vibeTitle: String? = null
)

@Serializable
data class SearchLogMovieSampleDto(
    @SerialName("title") val title: String,
    @SerialName("year") val year: String? = null
)
