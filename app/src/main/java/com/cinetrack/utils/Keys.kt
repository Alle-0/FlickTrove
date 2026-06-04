package com.cinetrack.utils

import com.cinetrack.BuildConfig

object Keys {
    fun getTmdbKey(): String = BuildConfig.TMDB_API_KEY
    fun getOmdbKey(): String = BuildConfig.OMDB_API_KEY
    fun getTraktKey(): String = BuildConfig.TRAKT_API_KEY
}
