package com.cinetrack.data.model

import kotlinx.serialization.Serializable

/**
 * Data class representing secondary ratings from external services (OMDb, Trakt).
 */
@Serializable
data class ExtraRatings(
    val imdbRating: String? = null,
    val imdbVotes: String? = null,
    val rottenTomatoes: String? = null,
    val metacritic: String? = null,
    val traktRating: String? = null,
    val awards: String? = null,
    val isLoading: Boolean = false
)
