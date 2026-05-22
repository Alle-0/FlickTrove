package com.cinetrack.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "color_cache")
data class ColorCacheEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "color_hex") val colorHex: String,
    @ColumnInfo(name = "ambient_hex") val ambientHex: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String
)

@Entity(tableName = "movie_details_cache", primaryKeys = ["id", "media_type"])
data class MovieDetailCacheEntity(
    val id: Int,
    @ColumnInfo(name = "media_type") val mediaType: String,
    val data: String, // Full JSON blob from API
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)
