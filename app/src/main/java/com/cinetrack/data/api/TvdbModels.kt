package com.cinetrack.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TvdbLoginRequest(
    val apikey: String
)

@Serializable
data class TvdbLoginResponse(
    val status: String,
    val data: TvdbTokenData? = null
)

@Serializable
data class TvdbTokenData(
    val token: String
)

@Serializable
data class TvdbSearchResponse(
    val status: String,
    val data: List<TvdbSearchResult> = emptyList()
)

@Serializable
data class TvdbSearchResult(
    val tvdb_id: String? = null,
    val name: String? = null,
    val year: String? = null
)

@Serializable
data class TvdbExtendedResponse(
    val status: String,
    val data: TvdbExtendedData? = null
)

@Serializable
data class TvdbExtendedData(
    val characters: List<TvdbCharacter> = emptyList(),
    val seasons: List<TvdbSeason> = emptyList(),
    val episodes: List<TvdbEpisode> = emptyList()
)

@Serializable
data class TvdbCharacter(
    val id: Long,
    val name: String? = null,
    val image: String? = null,
    val personName: String? = null
)

@Serializable
data class TvdbSeasonType(
    val id: Long,
    val name: String,
    val type: String
)

@Serializable
data class TvdbSeason(
    val id: Long,
    val seriesId: Long? = null,
    val type: TvdbSeasonType? = null,
    val name: String? = null,
    val number: Int? = null,
    val image: String? = null,
    val imageType: Int? = null
)

@Serializable
data class TvdbEpisode(
    val id: Long,
    val seriesId: Long? = null,
    val name: String? = null,
    val aired: String? = null,
    val runtime: Int? = null,
    val nameTranslations: List<String>? = null,
    val overview: String? = null,
    val overviewTranslations: List<String>? = null,
    val image: String? = null,
    val imageType: Int? = null,
    val isMovie: Int? = null,
    val seasons: List<TvdbSeason>? = null,
    val number: Int? = null,
    val absoluteNumber: Int? = null,
    val seasonNumber: Int? = null,
    val lastUpdated: String? = null,
    val finaleType: String? = null
)

@Serializable
data class TvdbLinks(
    val prev: String? = null,
    val next: String? = null,
    val self: String? = null,
    val total_items: Int? = null,
    val page_size: Int? = null
)

@Serializable
data class TvdbEpisodesResponse(
    val status: String,
    val data: TvdbEpisodesData? = null,
    val links: TvdbLinks? = null
)

@Serializable
data class TvdbEpisodesData(
    val episodes: List<TvdbEpisode> = emptyList()
)
