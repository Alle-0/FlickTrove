package com.cinetrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cinetrack.data.local.entities.BadgeGlobalStatsEntity
import com.cinetrack.data.local.entities.UserBadgeEntity
import com.cinetrack.data.local.entities.UserPersonFrequencyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BadgeDao {

    // ── GESTIONE BADGE UTENTE ──

    @Query("SELECT * FROM user_badges ORDER BY category ASC, is_unlocked DESC, progress_current DESC")
    fun getAllUserBadgesFlow(): Flow<List<UserBadgeEntity>>

    @Query("SELECT * FROM user_badges WHERE is_unlocked = 1 ORDER BY last_updated_at DESC")
    fun getUnlockedUserBadgesFlow(): Flow<List<UserBadgeEntity>>

    @Query("SELECT * FROM user_badges")
    suspend fun getAllUserBadges(): List<UserBadgeEntity>

    @Query("SELECT * FROM user_badges WHERE badge_id = :badgeId LIMIT 1")
    suspend fun getBadgeById(badgeId: String): UserBadgeEntity?

    @Query("SELECT * FROM user_badges WHERE badge_id = :badgeId LIMIT 1")
    fun getBadgeByIdFlow(badgeId: String): Flow<UserBadgeEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBadge(badge: UserBadgeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBadges(badges: List<UserBadgeEntity>)

    @Query("SELECT COUNT(*) FROM user_badges WHERE is_unlocked = 1")
    fun getUnlockedBadgesCountFlow(): Flow<Int>

    @Query("SELECT * FROM user_badges WHERE sync_status = 'pending'")
    suspend fun getPendingSyncBadges(): List<UserBadgeEntity>

    @Query("UPDATE user_badges SET sync_status = 'synced' WHERE badge_id IN (:badgeIds)")
    suspend fun markBadgesAsSynced(badgeIds: List<String>)


    // ── STATISTICHE GLOBALI DI RARITÀ ──

    @Query("SELECT * FROM badge_global_stats")
    fun getAllGlobalStatsFlow(): Flow<List<BadgeGlobalStatsEntity>>

    @Query("SELECT * FROM badge_global_stats WHERE badge_id = :badgeId LIMIT 1")
    suspend fun getGlobalStatsByBadgeId(badgeId: String): BadgeGlobalStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGlobalStats(stats: List<BadgeGlobalStatsEntity>)


    // ── FREQUENZA ARTISTI (Attori / Registi per Trofei #32 e #33) ──

    @Query("SELECT * FROM user_person_frequency WHERE role_type = :roleType ORDER BY watched_count DESC LIMIT :limit")
    suspend fun getTopPersonsByRole(roleType: String, limit: Int = 1): List<UserPersonFrequencyEntity>

    @Query("SELECT * FROM user_person_frequency WHERE person_id = :personId AND role_type = :roleType LIMIT 1")
    suspend fun getPersonFrequency(personId: Long, roleType: String): UserPersonFrequencyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPersonFrequency(frequency: UserPersonFrequencyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPersonFrequencies(frequencies: List<UserPersonFrequencyEntity>)

    @Query("DELETE FROM user_person_frequency")
    suspend fun clearPersonFrequencies()
}
