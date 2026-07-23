package com.cinetrack.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class TraktExportItem(
    val type: String? = null,
    val movie: TraktMovie? = null,
    val show: TraktShow? = null,
    val episode: TraktEpisode? = null,
    @SerialName("watched_at") val watchedAt: String? = null,
    val watched: Boolean? = null,
    val dropped: Boolean? = null,
    val status: String? = null,
    val rating: Float? = null,
    val notes: String? = null
)

@Serializable
data class TraktEpisode(
    val season: Int? = null,
    val number: Int? = null,
    val title: String? = null,
    val ids: TraktIds? = null
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
