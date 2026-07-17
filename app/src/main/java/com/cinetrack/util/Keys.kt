package com.cinetrack.util

import com.cinetrack.BuildConfig

object Keys {
    fun getTmdbKey(): String = BuildConfig.TMDB_API_KEY
    fun getOmdbKey(): String = BuildConfig.OMDB_API_KEY
    // Trakt Client ID — confirmed on Trakt developer dashboard (public, not a secret)
    fun getTraktKey(): String = "26c1520987a8cc340d46415540ca47e4c8e4964d09a88599d3b7b9e94fd870f5"
    fun getTraktSecret(): String = BuildConfig.TRAKT_CLIENT_SECRET
}
