package com.cinetrack.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface KlipyService {

    @GET("{appKey}/gifs/trending")
    suspend fun getTrending(
        @Path("appKey") appKey: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
        @Query("customer_id") customerId: String? = null,
        @Query("locale") locale: String? = null,
        @Query("content_filter") contentFilter: String = "medium",
        @Query("rating") rating: String = "pg-13"
    ): KlipyResponseDto

    @GET("{appKey}/gifs/search")
    suspend fun searchGifs(
        @Path("appKey") appKey: String,
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
        @Query("customer_id") customerId: String? = null,
        @Query("locale") locale: String? = null,
        @Query("content_filter") contentFilter: String = "medium",
        @Query("rating") rating: String = "pg-13"
    ): KlipyResponseDto

    @POST("{appKey}/gifs/share/{slug}")
    suspend fun shareGif(
        @Path("appKey") appKey: String,
        @Path("slug") slug: String,
        @Body body: KlipyShareRequest
    ): KlipyShareResponseDto

    @GET("{appKey}/stickers/trending")
    suspend fun getTrendingStickers(
        @Path("appKey") appKey: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
        @Query("customer_id") customerId: String? = null,
        @Query("locale") locale: String? = null,
        @Query("content_filter") contentFilter: String = "medium",
        @Query("rating") rating: String = "pg-13"
    ): KlipyResponseDto

    @GET("{appKey}/stickers/search")
    suspend fun searchStickers(
        @Path("appKey") appKey: String,
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
        @Query("customer_id") customerId: String? = null,
        @Query("locale") locale: String? = null,
        @Query("content_filter") contentFilter: String = "medium",
        @Query("rating") rating: String = "pg-13"
    ): KlipyResponseDto

    @POST("{appKey}/stickers/share/{slug}")
    suspend fun shareSticker(
        @Path("appKey") appKey: String,
        @Path("slug") slug: String,
        @Body body: KlipyShareRequest
    ): KlipyShareResponseDto
}

@Serializable
data class KlipyShareRequest(
    @SerialName("customer_id") val customerId: String,
    @SerialName("q") val q: String? = null
)

@Serializable
data class KlipyShareResponseDto(
    @SerialName("result") val result: Boolean = false
)

@Serializable
data class KlipyResponseDto(
    @SerialName("result") val result: Boolean = false,
    @SerialName("data") val data: KlipyPageDataDto? = null
)

@Serializable
data class KlipyPageDataDto(
    @SerialName("data") val data: List<KlipyMediaItemDto> = emptyList(),
    @SerialName("current_page") val currentPage: Int = 1,
    @SerialName("per_page") val perPage: Int = 20,
    @SerialName("has_next") val hasNext: Boolean = false
)

@Serializable
data class KlipyMediaItemDto(
    @SerialName("id") val id: Long = 0L,
    @SerialName("slug") val slug: String = "",
    @SerialName("type") val type: String = "gif",
    @SerialName("title") val title: String = "",
    @SerialName("file") val file: KlipyFileDto? = null
)

@Serializable
data class KlipyFileDto(
    @SerialName("hd") val hd: KlipyMediaVariantsDto? = null,
    @SerialName("md") val md: KlipyMediaVariantsDto? = null,
    @SerialName("sm") val sm: KlipyMediaVariantsDto? = null,
    @SerialName("xs") val xs: KlipyMediaVariantsDto? = null
)

@Serializable
data class KlipyMediaVariantsDto(
    @SerialName("gif") val gif: KlipyRenditionDto? = null,
    @SerialName("webp") val webp: KlipyRenditionDto? = null,
    @SerialName("png") val png: KlipyRenditionDto? = null
)

@Serializable
data class KlipyRenditionDto(
    @SerialName("url") val url: String = "",
    @SerialName("width") val width: Int = 0,
    @SerialName("height") val height: Int = 0,
    @SerialName("size") val size: Long = 0L
)
