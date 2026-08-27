package com.cinetrack.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "watch_history", indices = [Index("movieId")])
data class WatchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val movieId: Long,
    val watchedAt: String, // ISO-8601 string representation of the watch date
    val isRewatch: Boolean = false, // false if first watch, true if subsequent rewatch
    val syncStatus: String = "pending" // can be "pending", "synced", or "deleted"
)
