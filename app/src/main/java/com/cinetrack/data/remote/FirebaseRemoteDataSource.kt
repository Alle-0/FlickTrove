package com.cinetrack.data.remote

import com.cinetrack.data.model.Movie
import com.cinetrack.data.model.Folder
import com.cinetrack.data.local.entities.FolderEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
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

    private fun getPreferencesDoc(uid: String) =
        firestore.collection("users").document(uid).collection("settings").document("preferences")


    /**
     * Set movie data in Firestore. Uses "mediaType_id" as document ID for cross-platform compatibility.
     */
    suspend fun setMovie(movie: Movie) {
        val uid = userId ?: return
        val docId = "${movie.mediaType}_${movie.id}"
        try {
            getFavoritesCollection(uid).document(docId)
                .set(movie, SetOptions.merge())
                .await()
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
                val batch = firestore.batch()
                chunk.forEach { movie ->
                    val docId = "${movie.mediaType}_${movie.id}"
                    batch.set(collection.document(docId), movie, SetOptions.merge())
                }
                batch.commit().await()
            } catch (e: Exception) {
                android.util.Log.e("FirebaseRemoteDataSource", "Error committing batch of ${chunk.size} movies: ${e.message}", e)
            }
        }
    }

    suspend fun deleteMovie(movieId: Long, mediaType: String) {
        val uid = userId ?: return
        val docId = "${mediaType}_${movieId}"
        getFavoritesCollection(uid).document(docId)
            .delete()
            .await()
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

                val snapshot = query.get(Source.SERVER).await()
                if (snapshot.isEmpty) break

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
        getFoldersCollection(uid).document(folder.id)
            .set(folder, SetOptions.merge())
            .await()
    }

    suspend fun deleteFolder(folderId: String) {
        val uid = userId ?: return
        getFoldersCollection(uid).document(folderId)
            .delete()
            .await()
    }

    suspend fun fetchAllFolders(): List<Folder> {
        val uid = userId
        if (uid == null) return emptyList()
        
        val snapshot = getFoldersCollection(uid).get(Source.SERVER).await()
        val list = mutableListOf<Folder>()
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
        return list
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
}

