package com.cinetrack.data.model

import com.google.firebase.firestore.PropertyName

data class GlobalMovieStats(
    @get:PropertyName("total_vibes")
    @set:PropertyName("total_vibes")
    var totalVibes: Long = 0,

    @get:PropertyName("total_mvps")
    @set:PropertyName("total_mvps")
    var totalMvps: Long = 0,

    @get:PropertyName("vibes")
    @set:PropertyName("vibes")
    var vibes: Map<String, Long> = emptyMap(),

    @get:PropertyName("mvps")
    @set:PropertyName("mvps")
    var mvps: Map<String, Long> = emptyMap()
)
