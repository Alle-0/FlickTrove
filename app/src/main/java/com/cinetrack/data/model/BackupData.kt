package com.cinetrack.data.model

import com.cinetrack.data.local.entities.FolderEntity
import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val version: Int = 1,
    val exportDate: Long = System.currentTimeMillis(),
    val favorites: List<Movie> = emptyList(),
    val folders: List<FolderEntity> = emptyList(),
    val preferences: UserPreferences? = null
)
