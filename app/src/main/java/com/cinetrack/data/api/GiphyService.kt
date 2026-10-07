package com.cinetrack.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

interface GiphyService {

    @GET("gifs/trending")
    suspend fun getTrending(
        @Query("api_key") apiKey: String,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("rating") rating: String = "pg-13"
    ): GiphyResponseDto

    @GET("gifs/search")
    suspend fun searchGifs(
        @Query("api_key") apiKey: String,
        @Query("q") query: String,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("rating") rating: String = "pg-13"
    ): GiphyResponseDto
}

@Serializable
data class GiphyResponseDto(
    @SerialName("data") val data: List<GiphyGifDto> = emptyList(),
    @SerialName("pagination") val pagination: GiphyPaginationDto? = null
)

@Serializable
data class GiphyPaginationDto(
    @SerialName("total_count") val totalCount: Int = 0,
    @SerialName("count") val count: Int = 0,
    @SerialName("offset") val offset: Int = 0
)

@Serializable
data class GiphyGifDto(
    @SerialName("id") val id: String = "",
    @SerialName("title") val title: String = "",
    @SerialName("images") val images: GiphyImagesDto? = null
)

@Serializable
data class GiphyImagesDto(
    @SerialName("fixed_height") val fixedHeight: GiphyImageRenditionDto? = null,
    @SerialName("fixed_width") val fixedWidth: GiphyImageRenditionDto? = null,
    @SerialName("original") val original: GiphyImageRenditionDto? = null,
    @SerialName("downsized") val downsized: GiphyImageRenditionDto? = null,
    @SerialName("preview_gif") val previewGif: GiphyImageRenditionDto? = null
)

@Serializable
data class GiphyImageRenditionDto(
    @SerialName("url") val url: String = "",
    @SerialName("width") val width: String = "",
    @SerialName("height") val height: String = ""
)
