package com.cinetrack.data.model

import com.google.firebase.firestore.PropertyName

data class EditorialCollection(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val backdropPath: String? = null,
    val posterPath: String? = null,
    val items: List<EditorialCollectionItem> = emptyList(),
    val createdAt: String = ""
)

data class EditorialCollectionItem(
    @get:PropertyName("tmdbId") @set:PropertyName("tmdbId")
    var tmdbId: Long = 0,
    
    @get:PropertyName("mediaType") @set:PropertyName("mediaType")
    var mediaType: String = "",
    
    @get:PropertyName("timelineIndex") @set:PropertyName("timelineIndex")
    var timelineIndex: Int = 0,
    
    @get:PropertyName("addedAt") @set:PropertyName("addedAt")
    var addedAt: String = ""
)
