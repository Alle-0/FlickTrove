package com.cinetrack.data.repository

import com.cinetrack.data.model.EditorialCollection
import com.cinetrack.data.model.Movie
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EditorialCollectionRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val movieRepository: MovieRepository
) {

    suspend fun getCollection(collectionId: String): EditorialCollection? {
        return try {
            val doc = firestore.collection("editorial_collections").document(collectionId).get().await()
            if (doc.exists()) {
                doc.toObject(EditorialCollection::class.java)?.copy(id = doc.id)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getAllCollections(): List<EditorialCollection> {
        return try {
            val snapshot = firestore.collection("editorial_collections").get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(EditorialCollection::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getCollectionMovies(collectionId: String): List<Pair<Movie, Int>> = coroutineScope {
        val collection = getCollection(collectionId) ?: return@coroutineScope emptyList()
        
        val deferredMovies = collection.items.map { item ->
            async {
                val movie = movieRepository.getMediaDetails(item.tmdbId, item.mediaType == "tv")
                if (movie != null) {
                    Pair(movie, item.timelineIndex)
                } else null
            }
        }
        
        deferredMovies.awaitAll().filterNotNull()
    }
}
