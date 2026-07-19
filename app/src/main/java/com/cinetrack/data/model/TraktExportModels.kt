package com.cinetrack.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class TraktExportItem(
    val movie: TraktMovie? = null,
    val show: TraktShow? = null,
    @SerialName("watched_at") val watchedAt: String? = null,
    val watched: Boolean? = null,
    val rating: Float? = null,
    val notes: String? = null
)

@Serializable
data class TraktMovie(
    val title: String? = null,
    val year: Int? = null,
    val ids: TraktIds? = null
)

@Serializable
data class TraktShow(
    val title: String? = null,
    val year: Int? = null,
    val ids: TraktIds? = null
)

@Serializable
data class TraktIds(
    val trakt: Long? = null,
    val slug: String? = null,
    val tvdb: Long? = null,
    val imdb: String? = null,
    val tmdb: Long? = null,
    val tvrage: Long? = null
)
