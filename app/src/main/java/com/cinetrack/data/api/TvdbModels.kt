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
    val characters: List<TvdbCharacter> = emptyList()
)

@Serializable
data class TvdbCharacter(
    val id: Long,
    val name: String? = null,
    val image: String? = null,
    val personName: String? = null
)
