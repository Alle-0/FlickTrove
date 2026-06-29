package com.cinetrack.utils

import com.cinetrack.BuildConfig

object Keys {
    fun getTmdbKey(): String = BuildConfig.TMDB_API_KEY
    fun getOmdbKey(): String = BuildConfig.OMDB_API_KEY
    fun getTraktKey(): String = BuildConfig.TRAKT_API_KEY
    fun getTraktSecret(): String = BuildConfig.TRAKT_CLIENT_SECRET
    
    fun getTraktClientId(): String {
        return "26c1520987a8cc340d46415540ca47e4c8e4964d09a88599d3b7b9e94fd870f5"
    }
}
