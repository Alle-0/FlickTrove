package com.cinetrack.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface FlickRoute

@Serializable
object HomeRoute : FlickRoute

@Serializable
object VistiRoute : FlickRoute

@Serializable
data class DiscoverRoute(
    val type: String = "popular",
    val genreId: Long? = null,
    val genreName: String? = null
) : FlickRoute

@Serializable
data class UpdatesRoute(
    val startX: Float? = null,
    val startY: Float? = null
) : FlickRoute

@Serializable
object StatsRoute : FlickRoute



@Serializable
object RecommendationsRoute : FlickRoute

@Serializable
object SurpriseMeRoute : FlickRoute

@Serializable
data class DetailRoute(
    val id: Long,
    val mediaType: String // 'movie' or 'tv'
) : FlickRoute

@Serializable
data class PersonRoute(
    val id: Long,
    val profilePath: String? = null
) : FlickRoute

@Serializable
object SettingsRoute : FlickRoute

@Serializable
object LoginRoute : FlickRoute

@Serializable
object SplashRoute : FlickRoute

@Serializable
object FoldersRoute : FlickRoute

@Serializable
data class FolderDetailRoute(val folderId: String, val folderName: String, val folderColor: String? = null) : FlickRoute

@Serializable
data class SearchRoute(
    val startX: Float? = null,
    val startY: Float? = null,
    val initialGenreName: String? = null,
    val initialKeywordName: String? = null,
    val initialGenreId: Long? = null,
    val initialKeywordId: Long? = null
) : FlickRoute

@Serializable
object LogoAnimationRoute : FlickRoute

@Serializable
object EmptyRoute : FlickRoute
