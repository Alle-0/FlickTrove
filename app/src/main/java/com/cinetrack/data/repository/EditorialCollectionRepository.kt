package com.cinetrack.data.repository

import android.content.Context
import com.cinetrack.R
import com.cinetrack.data.model.EditorialCollection
import com.cinetrack.data.model.Movie
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EditorialCollectionRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val movieRepository: MovieRepository,
    @ApplicationContext private val context: Context
) {

    private fun localize(col: EditorialCollection): EditorialCollection {
        return when (col.id) {
            "marvel_mcu" -> col.copy(
                title = context.getString(R.string.editorial_mcu_title),
                description = context.getString(R.string.editorial_mcu_desc)
            )
            "star_wars_canon" -> col.copy(
                title = context.getString(R.string.editorial_star_wars_title),
                description = context.getString(R.string.editorial_star_wars_desc)
            )
            else -> col
        }
    }

    suspend fun getCollection(collectionId: String): EditorialCollection? {
        return try {
            val doc = firestore.collection("editorial_collections").document(collectionId).get().await()
            if (doc.exists()) {
                val col = doc.toObject(EditorialCollection::class.java)?.copy(id = doc.id)
                col?.let { localize(it) }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private val cacheMutex = kotlinx.coroutines.sync.Mutex()
    private var cachedCollections: List<EditorialCollection>? = null
    private var lastCacheTime: Long = 0L
    private val CACHE_TTL_MS = 24 * 60 * 60 * 1000L // 24 ore

    suspend fun getAllCollections(forceRefresh: Boolean = false): List<EditorialCollection> {
        val now = System.currentTimeMillis()
        cachedCollections?.let {
            if (!forceRefresh && (now - lastCacheTime) < CACHE_TTL_MS) return it
        }

        return cacheMutex.withLock {
            val cached = cachedCollections
            if (!forceRefresh && cached != null && (System.currentTimeMillis() - lastCacheTime) < CACHE_TTL_MS) {
                return@withLock cached
            }
            try {
                val snapshot = firestore.collection("editorial_collections").get().await()
                val result = snapshot.documents.mapNotNull { doc ->
                    val col = doc.toObject(EditorialCollection::class.java)?.copy(id = doc.id)
                    col?.let { localize(it) }
                }
                cachedCollections = result
                lastCacheTime = System.currentTimeMillis()
                result
            } catch (e: Exception) {
                e.printStackTrace()
                cachedCollections ?: emptyList()
            }
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
