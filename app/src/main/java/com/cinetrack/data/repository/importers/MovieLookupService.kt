package com.cinetrack.data.repository.importers

import com.cinetrack.data.model.Movie

interface MovieLookupService {
    suspend fun findByImdbId(imdbId: String): Movie?
    suspend fun findByTvdbId(tvdbId: String): Movie?
    suspend fun searchMediaWithYear(query: String, year: String?, isTv: Boolean = false): Movie?
}
