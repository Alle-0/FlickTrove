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
            "the_conjuring_universe" -> col.copy(
                title = context.getString(R.string.editorial_conjuring_title),
                description = context.getString(R.string.editorial_conjuring_desc)
            )
            "monsterverse" -> col.copy(
                title = context.getString(R.string.editorial_monsterverse_title),
                description = context.getString(R.string.editorial_monsterverse_desc)
            )
            "middle_earth" -> col.copy(
                title = context.getString(R.string.editorial_middle_earth_title),
                description = context.getString(R.string.editorial_middle_earth_desc)
            )
            "alien_universe" -> col.copy(
                title = context.getString(R.string.editorial_alien_title),
                description = context.getString(R.string.editorial_alien_desc)
            )
            else -> col
        }
    }

    private fun loadFromAssets(collectionId: String): EditorialCollection? {
        return try {
            val jsonStr = context.assets.open("editorial_collections/$collectionId.json").bufferedReader().use { it.readText() }
            val obj = org.json.JSONObject(jsonStr)
            val itemsArray = obj.getJSONArray("items")
            val items = (0 until itemsArray.length()).map { i ->
                val itemObj = itemsArray.getJSONObject(i)
                com.cinetrack.data.model.EditorialCollectionItem(
                    tmdbId = itemObj.optLong("tmdbId"),
                    mediaType = itemObj.optString("mediaType"),
                    timelineIndex = itemObj.optInt("timelineIndex"),
                    addedAt = itemObj.optString("addedAt"),
                    seasonScope = itemObj.optString("seasonScope").takeIf { it.isNotBlank() }
                )
            }
            val col = EditorialCollection(
                id = obj.optString("id", collectionId),
                title = obj.optString("title"),
                description = obj.optString("description"),
                backdropPath = obj.optString("backdropPath").takeIf { it.isNotBlank() },
                posterPath = obj.optString("posterPath").takeIf { it.isNotBlank() },
                items = items,
                createdAt = obj.optString("createdAt")
            )
            localize(col)
        } catch (e: Exception) {
            null
        }
    }

    private fun loadAllFromAssets(): List<EditorialCollection> {
        val collectionIds = listOf(
            "marvel_mcu",
            "star_wars_canon",
            "the_conjuring_universe",
            "monsterverse",
            "middle_earth",
            "alien_universe"
        )
        return collectionIds.mapNotNull { loadFromAssets(it) }
    }

    suspend fun getCollection(collectionId: String): EditorialCollection? {
        val now = System.currentTimeMillis()
        cachedCollections?.let { list ->
            if ((now - lastCacheTime) < CACHE_TTL_MS) {
                list.find { it.id == collectionId }?.let { return it }
            }
        }

        return try {
            val doc = firestore.collection("editorial_collections").document(collectionId).get().await()
            if (doc.exists()) {
                val col = doc.toObject(EditorialCollection::class.java)?.copy(id = doc.id)
                col?.let { localize(it) }
            } else {
                loadFromAssets(collectionId)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            loadFromAssets(collectionId)
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
                val finalResult = if (result.isNotEmpty()) result else loadAllFromAssets()
                cachedCollections = finalResult
                lastCacheTime = System.currentTimeMillis()
                finalResult
            } catch (e: Exception) {
                e.printStackTrace()
                cachedCollections ?: loadAllFromAssets()
            }
        }
    }

    suspend fun getCollectionMovies(collectionId: String): List<Pair<Movie, Int>> = coroutineScope {
        val collection = getCollection(collectionId) ?: return@coroutineScope emptyList()
        
        val deferredMovies = collection.items.map { item ->
            async {
                val movie = movieRepository.getMediaDetails(item.tmdbId, item.mediaType == "tv")
                if (movie != null) {
                    val scopedMovie = if (!item.seasonScope.isNullOrBlank()) {
                        movie.copy().apply {
                            this.logoPath = movie.logoPath
                            this.matchScore = movie.matchScore
                            this.seasonScope = item.seasonScope
                        }
                    } else movie
                    Pair(scopedMovie, item.timelineIndex)
                } else null
            }
        }
        
        deferredMovies.awaitAll().filterNotNull()
    }
}
