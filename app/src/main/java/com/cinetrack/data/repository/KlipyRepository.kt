package com.cinetrack.data.repository

import com.cinetrack.data.api.KlipyMediaItemDto
import com.cinetrack.data.api.KlipyService
import com.cinetrack.data.api.KlipyShareRequest
import com.cinetrack.data.model.GifItem
import com.cinetrack.data.model.KlipyMediaType
import com.cinetrack.util.Keys
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class KlipyPageResult(
    val items: List<GifItem>,
    val hasNext: Boolean
)

private data class CachedTrendingPage(
    val fetchedAtMs: Long,
    val result: KlipyPageResult
)

@Singleton
class KlipyRepository @Inject constructor(
    private val klipyService: KlipyService,
    private val preferenceRepository: PreferenceRepository
) {
    private val mutex = Mutex()
    private val cachedTrendingByType = mutableMapOf<KlipyMediaType, CachedTrendingPage>()
    private val oneHourMs = 60 * 60 * 1000L

    /** Fetches localized trending GIFs or Stickers, caching page 1 per media type in memory for 1 hour. */
    suspend fun getTrending(
        page: Int = 1,
        perPage: Int = 20,
        appKey: String = Keys.getKlipyKey(),
        mediaType: KlipyMediaType = KlipyMediaType.GIF
    ): Result<KlipyPageResult> = runCatching {
        require(appKey.isNotBlank()) { "KLIPY API key is missing" }
        val now = System.currentTimeMillis()

        if (page == 1) {
            mutex.withLock {
                val cached = cachedTrendingByType[mediaType]
                if (cached != null && (now - cached.fetchedAtMs) < oneHourMs) {
                    return@runCatching cached.result
                }
            }
        }

        val customerId = preferenceRepository.getOrCreateAnonymousGifClientId()
        val locale = resolveIsoCountryCode()
        val response = when (mediaType) {
            KlipyMediaType.GIF -> klipyService.getTrending(
                appKey = appKey,
                page = page,
                perPage = perPage,
                customerId = customerId,
                locale = locale
            )
            KlipyMediaType.STICKER -> klipyService.getTrendingStickers(
                appKey = appKey,
                page = page,
                perPage = perPage,
                customerId = customerId,
                locale = locale
            )
        }

        val pageData = response.data
        val pageResult = KlipyPageResult(
            items = pageData?.data.orEmpty().mapNotNull { mapToGifItem(it, mediaType) },
            hasNext = pageData?.hasNext ?: false
        )

        if (page == 1 && pageResult.items.isNotEmpty()) {
            mutex.withLock {
                cachedTrendingByType[mediaType] = CachedTrendingPage(now, pageResult)
            }
        }

        pageResult
    }

    /** Searches KLIPY GIFs or Stickers for [query] using 1-based pagination. */
    suspend fun searchGifs(
        query: String,
        page: Int = 1,
        perPage: Int = 20,
        appKey: String = Keys.getKlipyKey(),
        mediaType: KlipyMediaType = KlipyMediaType.GIF
    ): Result<KlipyPageResult> = runCatching {
        require(appKey.isNotBlank()) { "KLIPY API key is missing" }
        val customerId = preferenceRepository.getOrCreateAnonymousGifClientId()
        val locale = resolveIsoCountryCode()
        val response = when (mediaType) {
            KlipyMediaType.GIF -> klipyService.searchGifs(
                appKey = appKey,
                query = query,
                page = page,
                perPage = perPage,
                customerId = customerId,
                locale = locale
            )
            KlipyMediaType.STICKER -> klipyService.searchStickers(
                appKey = appKey,
                query = query,
                page = page,
                perPage = perPage,
                customerId = customerId,
                locale = locale
            )
        }

        val pageData = response.data
        KlipyPageResult(
            items = pageData?.data.orEmpty().mapNotNull { mapToGifItem(it, mediaType) },
            hasNext = pageData?.hasNext ?: false
        )
    }

    /** Reports an anonymous GIF or Sticker share trigger to improve KLIPY relevance; never throws. */
    suspend fun registerShare(
        slug: String,
        query: String?,
        appKey: String = Keys.getKlipyKey(),
        mediaType: KlipyMediaType = KlipyMediaType.GIF
    ) {
        if (slug.isBlank() || appKey.isBlank()) return
        runCatching {
            val body = KlipyShareRequest(
                customerId = preferenceRepository.getOrCreateAnonymousGifClientId(),
                q = query?.takeIf { it.isNotBlank() }
            )
            when (mediaType) {
                KlipyMediaType.GIF -> klipyService.shareGif(appKey = appKey, slug = slug, body = body)
                KlipyMediaType.STICKER -> klipyService.shareSticker(appKey = appKey, slug = slug, body = body)
            }
        }
    }

    private fun resolveIsoCountryCode(): String =
        Locale.getDefault().country.lowercase(Locale.ROOT).ifBlank { "us" }

    private fun mapToGifItem(dto: KlipyMediaItemDto, expectedType: KlipyMediaType): GifItem? {
        if (dto.type != expectedType.apiType || dto.file == null) return null

        val file = dto.file
        // Full URL strictly prefers GIF for federated CommsUni compatibility; static stickers may fall back to WebP/PNG
        val fullRendition = file.md?.gif?.takeIf { it.url.isNotBlank() }
            ?: file.hd?.gif?.takeIf { it.url.isNotBlank() }
            ?: file.sm?.gif?.takeIf { it.url.isNotBlank() }
            ?: if (expectedType == KlipyMediaType.STICKER) {
                file.md?.webp?.takeIf { it.url.isNotBlank() }
                    ?: file.hd?.webp?.takeIf { it.url.isNotBlank() }
                    ?: file.sm?.webp?.takeIf { it.url.isNotBlank() }
                    ?: file.md?.png?.takeIf { it.url.isNotBlank() }
                    ?: file.hd?.png?.takeIf { it.url.isNotBlank() }
                    ?: file.sm?.png?.takeIf { it.url.isNotBlank() }
            } else {
                null
            }
            ?: return null

        // Local grid preview prefers lightweight animated WebP with GIF/PNG fallback
        val previewRendition = file.sm?.webp?.takeIf { it.url.isNotBlank() }
            ?: file.sm?.gif?.takeIf { it.url.isNotBlank() }
            ?: file.md?.webp?.takeIf { it.url.isNotBlank() }
            ?: file.sm?.png?.takeIf { it.url.isNotBlank() }
            ?: fullRendition

        val stableId = dto.id.takeIf { it != 0L }?.toString() ?: dto.slug
        if (stableId.isBlank()) return null

        val width = previewRendition.width.takeIf { it > 0 }
            ?: fullRendition.width.takeIf { it > 0 }
            ?: 200
        val height = previewRendition.height.takeIf { it > 0 }
            ?: fullRendition.height.takeIf { it > 0 }
            ?: 200

        return GifItem(
            id = stableId,
            slug = dto.slug,
            title = dto.title,
            previewUrl = previewRendition.url,
            fullUrl = fullRendition.url,
            width = width,
            height = height,
            type = expectedType
        )
    }
}
