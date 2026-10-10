package com.cinetrack.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Cache locale per le percentuali globali di possesso dei trofei ("Sbloccato dal 4.2% dei cinefili").
 * Alimentata dal documento aggregato Firestore `/system_metrics/badges_global`.
 */
@Serializable
@Entity(tableName = "badge_global_stats")
data class BadgeGlobalStatsEntity(
    @PrimaryKey
    @ColumnInfo(name = "badge_id")
    val badgeId: String,

    @ColumnInfo(name = "rarity_percent")
    val rarityPercent: Float,                      // Valore percentuale da 0.1 a 100.0 (es. 4.2f)

    @ColumnInfo(name = "unlocked_users_count")
    val unlockedUsersCount: Long = 0L,             // Numero assoluto di cinefili che possiedono il badge

    @ColumnInfo(name = "total_users_count")
    val totalUsersCount: Long = 0L,                // Bacino totale utenti monitorato

    @ColumnInfo(name = "cached_at")
    val cachedAt: Long = System.currentTimeMillis() // Timestamp di aggiornamento della cache
)
