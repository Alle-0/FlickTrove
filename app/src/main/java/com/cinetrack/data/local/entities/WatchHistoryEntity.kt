package com.cinetrack.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "watch_history",
    indices = [
        Index("movieId"),
        Index(value = ["movieId", "mediaType", "watchedAt"], unique = true)
    ]
)
data class WatchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val movieId: Long,
    val mediaType: String = "movie",
    val watchedAt: String, // ISO-8601 string representation of the watch date
    val isRewatch: Boolean = false, // false if first watch, true if subsequent rewatch
    val syncStatus: String = "pending", // can be "pending", "synced", or "deleted"
    val historicalEpisodes: String? = null, // JSON map of watchedEpisodes snapshot
    val episodeDates: String? = null // JSON map of episode watch dates (e.g. "1_1" -> "2026-10-25T12:00:00Z")
)

