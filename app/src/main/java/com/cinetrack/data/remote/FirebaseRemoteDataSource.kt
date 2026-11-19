package com.cinetrack.data.remote

import com.cinetrack.data.model.Movie
import com.cinetrack.data.model.Folder
import com.cinetrack.data.local.entities.FolderEntity
import com.cinetrack.data.local.entities.WatchHistoryEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val globalStatsDataSource: FirebaseGlobalStatsDataSource
) {
    private val userId: String?
        get() = try {
            auth.currentUser?.uid
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRemoteDataSource", "Error accessing auth.currentUser: ${e.message}", e)
            null
        }

    private fun getFavoritesCollection(uid: String) =
        firestore.collection("users").document(uid).collection("favorite_movies")

    private fun getFoldersCollection(uid: String) =
        firestore.collection("users").document(uid).collection("folders")

    private fun getWatchHistoryCollection(uid: String) =
        firestore.collection("users").document(uid).collection("watch_history")

    private fun getPreferencesDoc(uid: String) =
        firestore.collection("users").document(uid).collection("settings").document("preferences")


    /**
     * Set movie data in Firestore. Uses "mediaType_id" as document ID for cross-platform compatibility.
     */
    suspend fun setMovie(movie: Movie) {
        val uid = userId ?: return
        val docId = "${movie.mediaType}_${movie.id}"
        try {
            kotlinx.coroutines.withTimeoutOrNull(8_000L) {
                getFavoritesCollection(uid).document(docId)
                    .set(movie, SetOptions.merge())
                    .await()
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRemoteDataSource", "Error setting movie $docId: ${e.message}", e)
        }
    }

    /**
     * Batch set movies in Firestore in smaller chunks of 40 to prevent local overlay cache and Protobuf serialization OOM.
     */
    suspend fun setMoviesBulk(movies: List<Movie>) {
        val uid = userId ?: return
        val collection = getFavoritesCollection(uid)
        movies.chunked(40).forEach { chunk ->
            try {
                kotlinx.coroutines.withTimeoutOrNull(10_000L) {
                    val batch = firestore.batch()
                    chunk.forEach { movie ->
                        val docId = "${movie.mediaType}_${movie.id}"
                        batch.set(collection.document(docId), movie, SetOptions.merge())
                    }
                    batch.commit().await()
                }
            } catch (e: Exception) {
                android.util.Log.e("FirebaseRemoteDataSource", "Error committing batch of ${chunk.size} movies: ${e.message}", e)
            }
        }
    }

    suspend fun deleteMovie(movieId: Long, mediaType: String) {
        val uid = userId ?: return
        val docId = "${mediaType}_${movieId}"
        val now = System.currentTimeMillis()
        try {
            kotlinx.coroutines.withTimeoutOrNull(6_000L) {
                getFavoritesCollection(uid).document(docId).set(
                    mapOf(
                        "id" to movieId,
                        "mediaType" to mediaType,
                        "sync_status" to "deleted",
                        "client_updated_at" to now,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                ).await()
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRemoteDataSource", "Error soft-deleting movie $docId: ${e.message}", e)
        }
    }

    suspend fun softDeleteFavorite(movieId: Long, mediaType: String) = deleteMovie(movieId, mediaType)

    suspend fun fetchFavoritesDelta(sinceTimestamp: Long): List<Movie> {
        val uid = userId ?: return emptyList()
        // Se è il primo avvio assoluto o cache azzerata, usa il fetch paginato sicuro
        if (sinceTimestamp <= 0L) {
            return fetchAllFavorites()
        }

        val list = mutableListOf<Movie>()
        try {
            // Interroga per client_updated_at coerente con il modello Movie
            val query = getFavoritesCollection(uid)
                .whereGreaterThan("client_updated_at", sinceTimestamp)

            val snapshot = kotlinx.coroutines.withTimeoutOrNull(15_000L) {
                query.get(Source.SERVER).await()
            }
            if (snapshot != null) {
                for (doc in snapshot.documents) {
                    try {
                        val movie = doc.toObject(Movie::class.java)
                        if (movie != null) {
                            list.add(movie)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("FirebaseRemoteDataSource", "Error deserializing delta movie document ${doc.id}: ${e.message}", e)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRemoteDataSource", "Error during delta fetch of favorites: ${e.message}", e)
        }
        return list
    }

    suspend fun fetchAllFavorites(): List<Movie> {
        val uid = userId
        if (uid == null) {
            android.util.Log.w("FirebaseRemoteDataSource", "fetchAllFavorites: No user ID found!")
            return emptyList()
        }
        
        android.util.Log.d("FirebaseRemoteDataSource", "Fetching favorites for UID: $uid in paginated chunks to prevent OOM")
        val list = mutableListOf<Movie>()
        var lastSnapshot: com.google.firebase.firestore.DocumentSnapshot? = null
        val pageSize = 35L

        while (true) {
            try {
                var query = getFavoritesCollection(uid)
                    .orderBy(FieldPath.documentId())
                    .limit(pageSize)
                if (lastSnapshot != null) {
                    query = query.startAfter(lastSnapshot)
                }

                val snapshot = kotlinx.coroutines.withTimeoutOrNull(12_000L) {
                    query.get(Source.SERVER).await()
                }
                if (snapshot == null || snapshot.isEmpty) break

                for (doc in snapshot.documents) {
                    try {
                        val movie = doc.toObject(Movie::class.java)
                        if (movie != null) {
                            list.add(movie)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("FirebaseRemoteDataSource", "Error deserializing movie document ${doc.id}: ${e.message}", e)
                    }
                }
                lastSnapshot = snapshot.documents.lastOrNull()
                if (snapshot.size() < pageSize.toInt()) break
            } catch (e: Exception) {
                android.util.Log.e("FirebaseRemoteDataSource", "Error during paginated fetch of favorites: ${e.message}", e)
                break
            }
        }
        return list
    }

    /**
     * Set folder data in Firestore.
     */
    suspend fun setFolder(folder: Folder) {
        val uid = userId ?: return
        if (folder.id.isBlank()) return
        try {
            kotlinx.coroutines.withTimeoutOrNull(6_000L) {
                getFoldersCollection(uid).document(folder.id)
                    .set(folder, SetOptions.merge())
                    .await()
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRemoteDataSource", "Error setting folder ${folder.id}: ${e.message}", e)
        }
    }

    suspend fun deleteFolder(folderId: String) {
        val uid = userId ?: return
        if (folderId.isBlank()) return
        try {
            kotlinx.coroutines.withTimeoutOrNull(6_000L) {
                getFoldersCollection(uid).document(folderId)
                    .delete()
                    .await()
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRemoteDataSource", "Error deleting folder $folderId: ${e.message}", e)
        }
    }

    /**
     * Wipes all user data (movies and folders) from Firestore.
     */
    suspend fun wipeAllCloudData() {
        val uid = userId ?: return

        // Wipe movies in batches
        try {
            val moviesSnapshot = getFavoritesCollection(uid).get().await()
            moviesSnapshot.documents.chunked(400).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { doc ->
                    batch.delete(doc.reference)
                }
                batch.commit().await()
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRemoteDataSource", "Error wiping cloud movies: ${e.message}", e)
        }

        // Wipe folders in batches
        try {
            val foldersSnapshot = getFoldersCollection(uid).get().await()
            foldersSnapshot.documents.chunked(400).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { doc ->
                    batch.delete(doc.reference)
                }
                batch.commit().await()
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRemoteDataSource", "Error wiping cloud folders: ${e.message}", e)
        }
    }

    suspend fun fetchAllFolders(): List<Folder> {
        val uid = userId
        if (uid == null) return emptyList()
        
        val list = mutableListOf<Folder>()
        try {
            val snapshot = kotlinx.coroutines.withTimeoutOrNull(12_000L) {
                getFoldersCollection(uid).get(Source.SERVER).await()
            } ?: return emptyList()
            for (doc in snapshot.documents) {
                try {
                    val folder = doc.toObject(Folder::class.java)
                    if (folder != null) {
                        list.add(folder)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("FirebaseRemoteDataSource", "Error deserializing folder document ${doc.id}: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRemoteDataSource", "Error fetching folders: ${e.message}", e)
        }
        return list
    }

    /**
     * Batch set Watch History in Firestore with client_updated_at for Delta Sync
     */
    suspend fun batchSetWatchHistory(entries: List<WatchHistoryEntity>) {
        val uid = userId ?: return
        val collection = getWatchHistoryCollection(uid)
        val now = System.currentTimeMillis()
        
        try {
            val chunks = entries.chunked(40)
            for (chunk in chunks) {
                kotlinx.coroutines.withTimeoutOrNull(10_000L) {
                    firestore.runBatch { batch ->
                        for (entry in chunk) {
                            val safeWatchedAt = entry.watchedAt.replace("/", "-")
                            val docId = if (entry.mediaType == "tv") "tv_${entry.movieId}_${safeWatchedAt}" else "${entry.movieId}_${safeWatchedAt}"
                            val docRef = collection.document(docId)
                            val map = mapOf(
                                "id" to entry.id,
                                "movieId" to entry.movieId,
                                "mediaType" to entry.mediaType,
                                "media_type" to entry.mediaType,
                                "watchedAt" to entry.watchedAt,
                                "isRewatch" to entry.isRewatch,
                                "syncStatus" to entry.syncStatus,
                                "sync_status" to entry.syncStatus,
                                "historicalEpisodes" to entry.historicalEpisodes,
                                "episodeDates" to entry.episodeDates,
                                "client_updated_at" to now
                            )
                            batch.set(docRef, map, SetOptions.merge())
                        }
                    }.await()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRemoteDataSource", "Error batch setting watch history: ${e.message}", e)
        }
    }

    /**
     * Fetch Watch History Delta from Firestore
     */
    suspend fun fetchWatchHistoryDelta(sinceTimestamp: Long): List<WatchHistoryEntity> {
        val uid = userId ?: return emptyList()
        val list = mutableListOf<WatchHistoryEntity>()
        try {
            val collection = getWatchHistoryCollection(uid)
            val query = if (sinceTimestamp > 0L) {
                collection.whereGreaterThan("client_updated_at", sinceTimestamp)
            } else {
                collection
            }
            val snapshot = query.get(Source.SERVER).await()
            for (doc in snapshot.documents) {
                val movieId = doc.getLong("movieId") ?: 0L
                val mediaType = doc.getString("mediaType")
                    ?: doc.getString("media_type")
                    ?: if (doc.getString("historicalEpisodes") != null || doc.id.startsWith("tv_")) "tv" else "movie"
                val watchedAt = doc.getString("watchedAt") ?: ""
                val isRewatch = doc.getBoolean("isRewatch") ?: false
                val syncStatus = doc.getString("sync_status") ?: doc.getString("syncStatus") ?: "synced"
                val historicalEpisodes = doc.getString("historicalEpisodes")
                val episodeDates = doc.getString("episodeDates")
                if (movieId > 0L && watchedAt.isNotBlank()) {
                    list.add(
                        WatchHistoryEntity(
                            id = 0,
                            movieId = movieId,
                            mediaType = mediaType,
                            watchedAt = watchedAt,
                            isRewatch = isRewatch,
                            syncStatus = syncStatus,
                            historicalEpisodes = historicalEpisodes,
                            episodeDates = episodeDates
                        )
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRemoteDataSource", "Error fetching watch history delta: ${e.message}", e)
        }
        return list
    }

    /**
     * Fetch all Watch History for the current user (Legacy full fetch)
     */
    suspend fun fetchAllWatchHistory(): List<WatchHistoryEntity> {
        return fetchWatchHistoryDelta(0L)
    }

    /**
     * Soft delete a single Watch History entry using Tombstone
     */
    suspend fun softDeleteWatchHistory(movieId: Long, watchedAt: String, mediaType: String = "movie") {
        val uid = userId ?: return
        val safeWatchedAt = watchedAt.replace("/", "-")
        val docId = if (mediaType == "tv") "tv_${movieId}_${safeWatchedAt}" else "${movieId}_${safeWatchedAt}"
        val now = System.currentTimeMillis()
        try {
            val tombstone = mapOf(
                "movieId" to movieId,
                "mediaType" to mediaType,
                "media_type" to mediaType,
                "watchedAt" to watchedAt,
                "syncStatus" to "deleted",
                "sync_status" to "deleted",
                "client_updated_at" to now
            )
            getWatchHistoryCollection(uid).document(docId).set(tombstone, SetOptions.merge()).await()
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRemoteDataSource", "Error soft-deleting watch history $docId: ${e.message}", e)
        }
    }

    suspend fun deleteWatchHistory(movieId: Long, watchedAt: String, mediaType: String = "movie") {
        softDeleteWatchHistory(movieId, watchedAt, mediaType)
    }

    /**
     * Batch soft-delete Watch History entries using Tombstones
     */
    suspend fun batchSoftDeleteWatchHistory(entries: List<WatchHistoryEntity>) {
        val uid = userId ?: return
        val collection = getWatchHistoryCollection(uid)
        val now = System.currentTimeMillis()
        try {
            val chunks = entries.chunked(100)
            for (chunk in chunks) {
                kotlinx.coroutines.withTimeoutOrNull(10_000L) {
                    firestore.runBatch { batch ->
                        for (entry in chunk) {
                            val safeWatchedAt = entry.watchedAt.replace("/", "-")
                            val docId = if (entry.mediaType == "tv") "tv_${entry.movieId}_${safeWatchedAt}" else "${entry.movieId}_${safeWatchedAt}"
                            val docRef = collection.document(docId)
                            val tombstone = mapOf(
                                "movieId" to entry.movieId,
                                "mediaType" to entry.mediaType,
                                "media_type" to entry.mediaType,
                                "watchedAt" to entry.watchedAt,
                                "syncStatus" to "deleted",
                                "sync_status" to "deleted",
                                "client_updated_at" to now
                            )
                            batch.set(docRef, tombstone, SetOptions.merge())
                        }
                    }.await()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRemoteDataSource", "Error batch soft-deleting watch history: ${e.message}", e)
        }
    }

    suspend fun batchDeleteWatchHistory(entries: List<WatchHistoryEntity>) {
        batchSoftDeleteWatchHistory(entries)
    }

    /**
     * User Preferences
     */
    suspend fun setUserPreferences(prefs: Map<String, Any>) {
        val uid = userId ?: return
        getPreferencesDoc(uid)
            .set(prefs, SetOptions.merge())
            .await()
    }

    suspend fun saveTasteVector(tasteVector: com.cinetrack.data.model.TasteVector): Boolean {
        val uid = userId ?: return false
        return try {
            kotlinx.coroutines.withTimeoutOrNull(10_000L) {
                firestore.collection("users").document(uid)
                    .collection("profile").document("taste_vector")
                    .set(tasteVector, SetOptions.merge())
                    .await()
                true
            } ?: false
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRemoteDataSource", "Error saving TasteVector for user " + uid + ": " + e.message, e)
            false
        }
    }

    suspend fun fetchUserPreferences(): Map<String, Any>? {
        val uid = userId
        if (uid == null) return null
        
        return try {
            val snapshot = getPreferencesDoc(uid).get(Source.SERVER).await()
            snapshot.data
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRemoteDataSource", "Error fetching preferences: ${e.message}", e)
            null
        }
    }
    // --- Global Movie Stats Delegated to FirebaseGlobalStatsDataSource ---

    fun getGlobalMovieStats(compositeId: String) =
        globalStatsDataSource.getGlobalMovieStats(compositeId)

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
    ) = globalStatsDataSource.updateGlobalMovieStats(
        compositeId = compositeId,
        addedVibes = addedVibes,
        removedVibes = removedVibes,
        newMvp = newMvp,
        oldMvp = oldMvp,
        newRating = newRating,
        oldRating = oldRating,
        newStatus = newStatus,
        oldStatus = oldStatus,
        viewsDelta = viewsDelta
    )

    data class TrendingStatUpdate(
        val compositeId: String,
        val viewsDelta: Long = 0L,
        val ratingDelta: Double = 0.0,
        val ratingCountDelta: Long = 0L
    )

    suspend fun updateTrendingStatsBulk(updates: List<TrendingStatUpdate>) =
        globalStatsDataSource.updateTrendingStatsBulk(updates)

    suspend fun fetchTop10MonthlyBoth(): Pair<List<String>, List<String>> =
        globalStatsDataSource.fetchTop10MonthlyBoth()

    suspend fun fetchTop10Monthly(isTv: Boolean): List<String> =
        globalStatsDataSource.fetchTop10Monthly(isTv)
}
