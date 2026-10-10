package com.cinetrack.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import kotlinx.serialization.Serializable

/**
 * Tabella snella di appoggio per tracciare la frequenza di visione di attori e registi
 * senza appesantire la tabella favorites né effettuare join complesse su JSON.
 * Usata per valutare i trofei "Attore Feticcio" e "Regista del Cuore".
 */
@Serializable
@Entity(
    tableName = "user_person_frequency",
    primaryKeys = ["person_id", "role_type"],
    indices = [
        Index(value = ["role_type", "watched_count"]),
        Index(value = ["watched_count"])
    ]
)
data class UserPersonFrequencyEntity(
    @ColumnInfo(name = "person_id")
    val personId: Long,                            // ID TMDB dell'attore o regista

    @ColumnInfo(name = "role_type")
    val roleType: String,                          // "ACTOR" o "DIRECTOR"

    @ColumnInfo(name = "person_name")
    val personName: String,

    @ColumnInfo(name = "profile_path")
    val profilePath: String? = null,

    @ColumnInfo(name = "watched_count")
    val watchedCount: Int = 0,                     // Quanti film/episodi visti coinvolgono questo artista

    @ColumnInfo(name = "last_watched_at")
    val lastWatchedAt: Long = System.currentTimeMillis()
)
