package com.cinetrack.utils

object Keys {
    init {
        System.loadLibrary("cinetrack")
    }

    external fun getTmdbKey(): String
    external fun getOmdbKey(): String
    external fun getTraktKey(): String
}
