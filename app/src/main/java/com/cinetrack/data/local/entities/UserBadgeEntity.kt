package com.cinetrack.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Entità Room che memorizza lo stato di conquista e progresso personale per ogni trofeo FlickTrove.
 */
@Serializable
@Entity(
    tableName = "user_badges",
    indices = [
        Index("current_tier"),
        Index("is_unlocked"),
        Index("category")
    ]
)
data class UserBadgeEntity(
    @PrimaryKey
    @ColumnInfo(name = "badge_id")
    val badgeId: String,                           // Identificativo univoco del badge (es. "badge_movies")

    @ColumnInfo(name = "current_tier")
    val currentTier: String,                       // "SUPER_8", "MM_16", "MM_35", "MM_70", "THE_FINAL_CUT", "LOST_REEL"

    @ColumnInfo(name = "category")
    val category: String,                          // "PROGRESSIVE_TIERS", "SPECIAL_ACHIEVEMENT", "DAY_ONE_HONOR", "LOST_REEL_SECRET"

    @ColumnInfo(name = "progress_current")
    val progressCurrent: Int = 0,                  // Avanzamento numerico attuale

    @ColumnInfo(name = "progress_target")
    val progressTarget: Int = 1,                   // Soglia richiesta per il tier attuale / prossimo

    @ColumnInfo(name = "is_unlocked")
    val isUnlocked: Boolean = false,               // true se ha raggiunto almeno il tier iniziale (Super 8 / The Final Cut)

    @ColumnInfo(name = "unlocked_date")
    val unlockedDate: String? = null,              // Data di conquista formattata o ISO-8601

    @ColumnInfo(name = "is_revealed")
    val isRevealed: Boolean = false,               // Per trofei segreti Lost Reel: true se l'utente l'ha scoperto

    @ColumnInfo(name = "sync_status")
    val syncStatus: String = "synced",             // "pending", "synced"

    @ColumnInfo(name = "last_updated_at")
    val lastUpdatedAt: Long = System.currentTimeMillis()
)
