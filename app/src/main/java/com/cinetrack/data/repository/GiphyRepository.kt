package com.cinetrack.data.repository

import com.cinetrack.data.api.GiphyService
import com.cinetrack.data.model.GiphyItem
import com.cinetrack.util.Keys
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GiphyRepository @Inject constructor(
    private val giphyService: GiphyService
) {
    private val mutex = Mutex()
    private var cachedTrending: List<GiphyItem>? = null
    private var lastTrendingFetchTime: Long = 0L
    private val ONE_HOUR_MS = 60 * 60 * 1000L

    suspend fun getTrending(limit: Int = 20, offset: Int = 0): Result<List<GiphyItem>> = runCatching {
        val now = System.currentTimeMillis()

        // 1-Hour in-memory cache check for initial page
        if (offset == 0) {
            mutex.withLock {
                val cached = cachedTrending
                if (cached != null && (now - lastTrendingFetchTime) < ONE_HOUR_MS) {
                    return@runCatching cached
                }
            }
        }

        val response = giphyService.getTrending(
            apiKey = Keys.getGiphyKey(),
            limit = limit,
            offset = offset
        )

        val items = response.data.mapNotNull { dto ->
            val fixedHeight = dto.images?.fixedHeight
            val original = dto.images?.original
            val url = fixedHeight?.url?.takeIf { it.isNotBlank() } ?: original?.url?.takeIf { it.isNotBlank() }
            if (url != null) {
                GiphyItem(
                    id = dto.id,
                    title = dto.title,
                    previewUrl = url,
                    fullUrl = url,
                    width = fixedHeight?.width?.toIntOrNull() ?: original?.width?.toIntOrNull() ?: 200,
                    height = fixedHeight?.height?.toIntOrNull() ?: original?.height?.toIntOrNull() ?: 200
                )
            } else null
        }

        if (offset == 0 && items.isNotEmpty()) {
            mutex.withLock {
                cachedTrending = items
                lastTrendingFetchTime = now
            }
        }

        items
    }

    suspend fun searchGifs(query: String, limit: Int = 20, offset: Int = 0): Result<List<GiphyItem>> = runCatching {
        val response = giphyService.searchGifs(
            apiKey = Keys.getGiphyKey(),
            query = query,
            limit = limit,
            offset = offset
        )

        response.data.mapNotNull { dto ->
            val fixedHeight = dto.images?.fixedHeight
            val original = dto.images?.original
            val url = fixedHeight?.url?.takeIf { it.isNotBlank() } ?: original?.url?.takeIf { it.isNotBlank() }
            if (url != null) {
                GiphyItem(
                    id = dto.id,
                    title = dto.title,
                    previewUrl = url,
                    fullUrl = url,
                    width = fixedHeight?.width?.toIntOrNull() ?: original?.width?.toIntOrNull() ?: 200,
                    height = fixedHeight?.height?.toIntOrNull() ?: original?.height?.toIntOrNull() ?: 200
                )
            } else null
        }
    }
}
