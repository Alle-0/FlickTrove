package com.cinetrack.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET

@Serializable
data class GithubRelease(
    @SerialName("tag_name") val tagName: String = "",
    val name: String? = null,
    val body: String? = null,
    @SerialName("html_url") val htmlUrl: String = "https://github.com/Alle-0/FlickTrove/releases/latest"
)

interface GithubService {
    @GET("repos/Alle-0/FlickTrove/releases/latest")
    suspend fun getLatestRelease(): GithubRelease
}
