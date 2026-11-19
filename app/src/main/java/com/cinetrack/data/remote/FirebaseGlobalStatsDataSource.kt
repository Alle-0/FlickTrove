package com.cinetrack.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class FirebaseGlobalStatsDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val userId: String?
        get() = try {
            auth.currentUser?.uid
        } catch (e: Exception) {
            android.util.Log.e("FirebaseGlobalStatsDataSource", "Error accessing auth.currentUser: ${e.message}", e)
            null
        }

    @Volatile private var cachedBanCheckUid: String? = null
    @Volatile private var cachedBannedFromVoting: Boolean = false
    @Volatile private var lastBanCheckTimestamp: Long = 0L
    private val BAN_CHECK_TTL_MS = 60 * 60 * 1000L // 1 ora di cache in memoria per azzerare letture Firestore su users/{uid}

    private suspend fun isUserBannedFromVoting(uid: String): Boolean {
        val now = System.currentTimeMillis()
        if (cachedBanCheckUid == uid && (now - lastBanCheckTimestamp) < BAN_CHECK_TTL_MS) {
            return cachedBannedFromVoting
        }
        return try {
            val userDoc = firestore.collection("users").document(uid).get().await()
            val isBanned = userDoc.getBoolean("bannedFromVoting") == true
            cachedBanCheckUid = uid
            cachedBannedFromVoting = isBanned
            lastBanCheckTimestamp = now
            isBanned
        } catch (e: Exception) {
            android.util.Log.e("FirebaseGlobalStatsDataSource", "Error checking ban status: ${e.message}", e)
            cachedBannedFromVoting
        }
    }

    fun getGlobalMovieStats(compositeId: String) =
        firestore.collection("global_movie_stats").document(compositeId)

    suspend fun updateGlobalMovieStats(
        compositeId: String,
        addedVibes: List<String>,
        removedVibes: List<String>,
        newMvp: Long?,
        oldMvp: Long?,
        newRating: Double? = null,
        oldRating: Double? = null,
        newStatus: String? = null,
        oldStatus: String? = null,
        viewsDelta: Long = 0L
    ) {
        val uid = userId ?: return
        if (isUserBannedFromVoting(uid)) {
            android.util.Log.w("FirebaseGlobalStatsDataSource", "User $uid is banned from voting. Shadow banning global stats update.")
            return
        }

        val docRef = getGlobalMovieStats(compositeId)
        try {
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)

                var countDelta = 0L
                var ratingDelta = 0.0
                if (snapshot.exists()) {
                    val updates = mutableMapOf<String, Any>()
                    var needsUpdate = false

                    var totalVibesDelta = 0L
                    addedVibes.forEach { vibe ->
                        updates["vibes.$vibe"] = FieldValue.increment(1)
                        totalVibesDelta++
                    }
                    removedVibes.forEach { vibe ->
                        updates["vibes.$vibe"] = FieldValue.increment(-1)
                        totalVibesDelta--
                    }
                    if (addedVibes.isNotEmpty() || removedVibes.isNotEmpty()) needsUpdate = true

                    if (totalVibesDelta != 0L) {
                        val currentTotalVibes = snapshot.getLong("total_vibes") ?: 0L
                        updates["total_vibes"] = (currentTotalVibes + totalVibesDelta).coerceAtLeast(0L)
                    }

                    var totalMvpDelta = 0L
                    if (newMvp != null && newMvp != oldMvp) {
                        updates["mvps.$newMvp"] = FieldValue.increment(1)
                        totalMvpDelta++
                    }
                    if (oldMvp != null && oldMvp != newMvp) {
                        updates["mvps.$oldMvp"] = FieldValue.increment(-1)
                        totalMvpDelta--
                    }
                    if (newMvp != oldMvp) needsUpdate = true

                    if (totalMvpDelta != 0L) {
                        val currentTotalMvps = snapshot.getLong("total_mvps") ?: 0L
                        updates["total_mvps"] = (currentTotalMvps + totalMvpDelta).coerceAtLeast(0L)
                    }

                    if (newRating != oldRating) {
                        val sanitizedOld = oldRating?.coerceIn(0.0, 10.0)
                        val sanitizedNew = newRating?.coerceIn(0.0, 10.0)
                        if (sanitizedOld != null && sanitizedOld > 0.0) {
                            ratingDelta -= sanitizedOld
                            countDelta -= 1L
                        }
                        if (sanitizedNew != null && sanitizedNew > 0.0) {
                            ratingDelta += sanitizedNew
                            countDelta += 1L
                        }
                        needsUpdate = true
                    }

                    if (ratingDelta != 0.0 || countDelta != 0L) {
                        val currentCount = snapshot.getLong("rating_count") ?: 0L
                        val newCount = (currentCount + countDelta).coerceAtLeast(0L)
                        val currentTotalRating = snapshot.getDouble("total_rating") ?: 0.0
                        val maxAllowedTotal = newCount * 10.0
                        val newTotalRating = if (newCount == 0L) 0.0 else (currentTotalRating + ratingDelta).coerceIn(0.0, maxAllowedTotal)

                        if (countDelta != 0L) {
                            updates["rating_count"] = newCount
                        }
                        updates["total_rating"] = newTotalRating
                    }

                    if (needsUpdate) {
                        transaction.update(docRef, updates)
                    }
                } else {
                    val initialData = mutableMapOf<String, Any>()

                    val vibesMap = mutableMapOf<String, Long>()
                    addedVibes.forEach { vibe -> vibesMap[vibe] = 1L }

                    if (vibesMap.isNotEmpty()) {
                        initialData["vibes"] = vibesMap
                        initialData["total_vibes"] = vibesMap.size.toLong()
                    }

                    val mvpsMap = mutableMapOf<String, Long>()
                    if (newMvp != null) {
                        mvpsMap[newMvp.toString()] = 1L
                    }

                    if (mvpsMap.isNotEmpty()) {
                        initialData["mvps"] = mvpsMap
                        initialData["total_mvps"] = 1L
                    }

                    if (newRating != null && newRating > 0.0) {
                        val sanitizedRating = newRating.coerceIn(0.0, 10.0)
                        initialData["total_rating"] = sanitizedRating
                        initialData["rating_count"] = 1L
                        countDelta = 1L
                        ratingDelta = sanitizedRating
                    }

                    if (initialData.isNotEmpty()) {
                        transaction.set(docRef, initialData)
                    }
                }

                // Update trending_stats_weekly
                val isNewWatched = (newStatus == "watched" && oldStatus != "watched")
                val isRemovedWatched = (oldStatus == "watched" && newStatus != "watched")

                var finalViewsDelta = viewsDelta
                if (compositeId.startsWith("movie_") && viewsDelta == 0L) {
                    if (isNewWatched) finalViewsDelta = 1L
                    if (isRemovedWatched) finalViewsDelta = -1L
                }

                if (countDelta != 0L || ratingDelta != 0.0 || finalViewsDelta != 0L) {
                    val weekId = com.cinetrack.util.DateTimeUtils.getIsoWeekId()
                    val subcollection = if (compositeId.startsWith("tv_")) "tv" else "movies"

                    val weeklyRef = firestore.collection("trending_stats_weekly")
                        .document(weekId)
                        .collection(subcollection)
                        .document(compositeId)

                    val trendingUpdates = mutableMapOf<String, Any>()
                    if (countDelta != 0L) {
                        trendingUpdates["rating_count"] = FieldValue.increment(countDelta)
                    }
                    if (ratingDelta != 0.0) {
                        trendingUpdates["total_rating"] = FieldValue.increment(ratingDelta)
                    }
                    if (finalViewsDelta != 0L) {
                        trendingUpdates["view_count"] = FieldValue.increment(finalViewsDelta)
                    }

                    if (trendingUpdates.isNotEmpty()) {
                        transaction.set(weeklyRef, trendingUpdates, SetOptions.merge())
                    }
                }

            }.await()
        } catch (e: Exception) {
            android.util.Log.e("FirebaseGlobalStatsDataSource", "Error updating global stats for $compositeId", e)
        }
    }

    suspend fun updateTrendingStatsBulk(updates: List<FirebaseRemoteDataSource.TrendingStatUpdate>) {
        if (updates.isEmpty()) return
        val uid = userId ?: return
        if (isUserBannedFromVoting(uid)) {
            android.util.Log.w("FirebaseGlobalStatsDataSource", "User $uid is banned from voting. Shadow banning bulk stats update.")
            return
        }

        val weekId = com.cinetrack.util.DateTimeUtils.getIsoWeekId()

        // Pre-aggregate locally by compositeId to avoid duplicate operations in batch
        val aggregated = mutableMapOf<String, FirebaseRemoteDataSource.TrendingStatUpdate>()
        for (update in updates) {
            if (update.viewsDelta == 0L && update.ratingCountDelta == 0L && update.ratingDelta == 0.0) continue
            val existing = aggregated[update.compositeId]
            if (existing != null) {
                aggregated[update.compositeId] = existing.copy(
                    viewsDelta = existing.viewsDelta + update.viewsDelta,
                    ratingDelta = existing.ratingDelta + update.ratingDelta,
                    ratingCountDelta = existing.ratingCountDelta + update.ratingCountDelta
                )
            } else {
                aggregated[update.compositeId] = update
            }
        }

        val operations = aggregated.values.toList()

        // Firestore limits batches to 500 operations.
        // We write to 1 document (weekly) per update, so max 450 updates per batch.
        val batchSize = 450
        for (i in operations.indices step batchSize) {
            val chunk = operations.subList(i, min(i + batchSize, operations.size))
            val batch = firestore.batch()

            for (update in chunk) {
                val subcollection = if (update.compositeId.startsWith("tv_")) "tv" else "movies"
                val weeklyRef = firestore.collection("trending_stats_weekly")
                    .document(weekId)
                    .collection(subcollection)
                    .document(update.compositeId)

                val trendingUpdates = mutableMapOf<String, Any>()
                if (update.ratingCountDelta != 0L) {
                    trendingUpdates["rating_count"] = FieldValue.increment(update.ratingCountDelta)
                }
                if (update.ratingDelta != 0.0) {
                    trendingUpdates["total_rating"] = FieldValue.increment(update.ratingDelta)
                }
                if (update.viewsDelta != 0L) {
                    trendingUpdates["view_count"] = FieldValue.increment(update.viewsDelta)
                }

                if (trendingUpdates.isNotEmpty()) {
                    batch.set(weeklyRef, trendingUpdates, SetOptions.merge())
                }
            }

            try {
                batch.commit().await()
            } catch (e: Exception) {
                android.util.Log.e("FirebaseGlobalStatsDataSource", "Error committing bulk stats batch", e)
            }
        }
    }

    suspend fun fetchTop10MonthlyBoth(): Pair<List<String>, List<String>> {
        val top10DocRef = firestore.collection("global_movie_stats").document("top10")

        // 1. Prova prima a leggere il documento pre-aggregato globale (1 SOLA LETTURA FIRESTORE!)
        try {
            val globalDoc = top10DocRef.get().await()
            if (globalDoc.exists()) {
                val updatedAt = globalDoc.getTimestamp("updatedAt")?.toDate()?.time ?: 0L
                val isRecent = (System.currentTimeMillis() - updatedAt) < (24 * 60 * 60 * 1000L) // 24 ore
                val movies = (globalDoc.get("top10_movies") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                val tv = (globalDoc.get("top10_tv") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

                if (isRecent && movies.isNotEmpty() && tv.isNotEmpty()) {
                    return Pair(movies, tv)
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("FirebaseGlobalStatsDataSource", "Could not fetch pre-aggregated top10, falling back to calculation", e)
        }

        // 2. Se il documento non esiste o è scaduto da oltre 24 ore, esegue il calcolo rolling 4 settimane
        val weekIds = com.cinetrack.util.DateTimeUtils.getRecentIsoWeekIds(weeksCount = 4)

        return try {
            val (movieSnaps, tvSnaps) = coroutineScope {
                val movieDeferred = weekIds.map { weekId ->
                    async {
                        try {
                            firestore.collection("trending_stats_weekly")
                                .document(weekId)
                                .collection("movies")
                                .orderBy("view_count", Query.Direction.DESCENDING)
                                .limit(30)
                                .get()
                                .await()
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
                val tvDeferred = weekIds.map { weekId ->
                    async {
                        try {
                            firestore.collection("trending_stats_weekly")
                                .document(weekId)
                                .collection("tv")
                                .orderBy("view_count", Query.Direction.DESCENDING)
                                .limit(20)
                                .get()
                                .await()
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
                Pair(movieDeferred.awaitAll().filterNotNull(), tvDeferred.awaitAll().filterNotNull())
            }

            data class ScoredItem(val compositeId: String, val score: Long)
            val mergedMovieItems = mutableMapOf<String, Long>()
            val mergedTvItems = mutableMapOf<String, Long>()

            for (snap in movieSnaps) {
                for (doc in snap.documents) {
                    val views = doc.getLong("view_count") ?: 0L
                    val ratings = doc.getLong("rating_count") ?: 0L
                    val score = views + (ratings * 2)
                    if (score > 0) {
                        if (doc.id.startsWith("movie_")) {
                            mergedMovieItems[doc.id] = (mergedMovieItems[doc.id] ?: 0L) + score
                        } else if (doc.id.startsWith("tv_")) {
                            mergedTvItems[doc.id] = (mergedTvItems[doc.id] ?: 0L) + score
                        }
                    }
                }
            }

            for (snap in tvSnaps) {
                for (doc in snap.documents) {
                    val views = doc.getLong("view_count") ?: 0L
                    val ratings = doc.getLong("rating_count") ?: 0L
                    val score = views + (ratings * 2)
                    if (score > 0) {
                        mergedTvItems[doc.id] = (mergedTvItems[doc.id] ?: 0L) + score
                    }
                }
            }

            val topMovies = mergedMovieItems.entries
                .map { ScoredItem(it.key, it.value) }
                .sortedByDescending { it.score }
                .take(10)
                .map { it.compositeId }

            val topTv = mergedTvItems.entries
                .map { ScoredItem(it.key, it.value) }
                .sortedByDescending { it.score }
                .take(10)
                .map { it.compositeId }

            // 3. Salva il nuovo calcolo nel documento globale per tutti gli altri utenti
            try {
                val updates = mutableMapOf<String, Any>(
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (topMovies.isNotEmpty()) updates["top10_movies"] = topMovies
                if (topTv.isNotEmpty()) updates["top10_tv"] = topTv

                top10DocRef.set(updates, SetOptions.merge())
            } catch (e: Exception) {
                android.util.Log.w("FirebaseGlobalStatsDataSource", "Failed to cache global top10 document", e)
            }

            Pair(topMovies, topTv)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseGlobalStatsDataSource", "Error fetching Top 10 Rolling 30 Days", e)
            Pair(emptyList(), emptyList())
        }
    }

    suspend fun fetchTop10Monthly(isTv: Boolean): List<String> {
        val (movies, tv) = fetchTop10MonthlyBoth()
        return if (isTv) tv else movies
    }
}
