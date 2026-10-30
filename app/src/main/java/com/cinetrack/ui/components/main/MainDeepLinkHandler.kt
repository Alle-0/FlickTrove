package com.cinetrack.ui.components.main

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.ui.geometry.Offset
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.TabNavigator
import com.cinetrack.ui.screens.CollectionDetailScreen
import com.cinetrack.ui.screens.DiscoverTab
import com.cinetrack.ui.screens.MovieDetailScreen
import com.cinetrack.ui.screens.PersonDetailScreen
import com.cinetrack.ui.screens.TrophyRoomTab
import com.cinetrack.ui.screens.VistiTab
import com.cinetrack.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay

@Composable
fun MainDeepLinkHandler(
    deepLinkIntent: MutableState<Intent?>,
    rootNavigator: Navigator,
    tabNavigator: TabNavigator,
    settingsViewModel: SettingsViewModel,
    searchOverlay: com.cinetrack.ui.SearchOverlayTrigger?
) {
    LaunchedEffect(deepLinkIntent.value) {
        val intent = deepLinkIntent.value ?: return@LaunchedEffect
        val uri = intent.data
        val action = intent.action
        
        // 1. LOG DI PARTENZA: Vediamo se l'intento arriva e cosa contiene
        println("FlickTroveDebug: LaunchedEffect intercettato! URI = $uri | Action = $action")

        if (uri != null || action?.startsWith("com.cinetrack.") == true) {
            if (uri != null) {
                delay(50)
            }

            val isCustomScheme = uri != null && uri.scheme == "flicktrove" && (uri.host == "media" || uri.host == "detail")

            val isHttpsAppLink = uri?.scheme == "https" && 
                    uri.host?.lowercase() == "alle-0.github.io" && 
                    uri.path?.contains("open.html", ignoreCase = true) == true

            if (isHttpsAppLink) {
                val type = uri.getQueryParameter("type") ?: "movie"
                val idStr = uri.getQueryParameter("id")
                val code = uri.getQueryParameter("code")
                
                if (type == "folder" || !code.isNullOrBlank()) {
                    val joinCode = code ?: idStr
                    if (!joinCode.isNullOrBlank()) {
                        com.cinetrack.ui.screens.FoldersTab.pendingJoinCode = joinCode
                        tabNavigator.current = com.cinetrack.ui.screens.FoldersTab
                    }
                } else if (!idStr.isNullOrBlank()) {
                    when (type) {
                        "person" -> idStr.toLongOrNull()?.let { rootNavigator.push(PersonDetailScreen(it, null)) }
                        "collection" -> rootNavigator.push(CollectionDetailScreen(idStr, null))
                        else -> idStr.toLongOrNull()?.let { rootNavigator.push(MovieDetailScreen(it, type)) }
                    }
                }
            } else if (uri?.scheme == "flicktrove" && (uri.host == "folder" || uri.host == "join")) {
                val code = uri.getQueryParameter("code") ?: uri.lastPathSegment
                if (!code.isNullOrBlank()) {
                    com.cinetrack.ui.screens.FoldersTab.pendingJoinCode = code
                    tabNavigator.current = com.cinetrack.ui.screens.FoldersTab
                }
            } else if (uri?.scheme == "flicktrove" && uri.host == "auth") {
                // Trakt OAuth callback: flicktrove://auth?code=XXXXX&state=YYYYY
                val code = uri.getQueryParameter("code")
                val returnedState = uri.getQueryParameter("state")
                if (!code.isNullOrEmpty()) {
                    settingsViewModel.exchangeTraktCode(code, returnedState)
                }
            } else if (uri?.scheme == "flicktrove" && uri.host == "simkl_login") {
                // SIMKL OAuth callback: flicktrove://simkl_login?code=XXXXX&state=YYYYY
                val code = uri.getQueryParameter("code")
                val returnedState = uri.getQueryParameter("state")
                if (!code.isNullOrEmpty()) {
                    settingsViewModel.exchangeSimklCode(code, returnedState)
                }
            } else if (isCustomScheme) {
                val pathSegments = uri.pathSegments
                val typeFromPath = if (pathSegments.size >= 2) pathSegments[0] else null
                val idStrFromPath = if (pathSegments.size >= 2) pathSegments[1] else null

                val type = uri.getQueryParameter("type") ?: typeFromPath ?: "movie"
                val idStr = uri.getQueryParameter("id") ?: idStrFromPath
                val openComments = uri.getQueryParameter("openComments")?.toBoolean() ?: false
                val targetCommentId = uri.getQueryParameter("commentId")

                if (!idStr.isNullOrBlank()) {
                    when (type) {
                        "person" -> idStr.toLongOrNull()?.let { rootNavigator.push(PersonDetailScreen(it, null)) }
                        "collection" -> rootNavigator.push(CollectionDetailScreen(idStr, null))
                        else -> idStr.toLongOrNull()?.let { rootNavigator.push(MovieDetailScreen(it, type, openComments = openComments, targetCommentId = targetCommentId)) }
                    }
                }
            } else if (uri?.scheme == "flicktrove" && uri.host == "search") {
                searchOverlay?.invoke(Offset(540f, 1140f), null, null, null, null)
            } else if (action == "com.cinetrack.SHORTCUT_SEARCH") {
                searchOverlay?.invoke(Offset(540f, 1140f), null, null, null, null)
            } else if (action == "com.cinetrack.SHORTCUT_VISTI") {
                tabNavigator.current = VistiTab
            } else if (action == "com.cinetrack.SHORTCUT_UPCOMING_MOVIES") {
                DiscoverTab.requestedType = "upcoming_movies"
                tabNavigator.current = DiscoverTab
            } else if (action == "com.cinetrack.SHORTCUT_UPCOMING_TV") {
                DiscoverTab.requestedType = "on_the_air_tv"
                tabNavigator.current = DiscoverTab
            } else if (action == "com.cinetrack.OPEN_DISCOVER_TAB") {
                val reqType = intent.getStringExtra("requestedType") ?: "popular_movies"
                DiscoverTab.requestedType = reqType
                tabNavigator.current = DiscoverTab
            } else if (action == "com.cinetrack.OPEN_TROPHY_ROOM") {
                val targetBadgeId = intent.getStringExtra("targetBadgeId")
                TrophyRoomTab.targetBadgeId = targetBadgeId
                tabNavigator.current = TrophyRoomTab
            } else if (action == "com.cinetrack.FLICKTROVE_SOCIAL_NOTIFICATION") {
                val mediaId = intent.getStringExtra("mediaId")?.toLongOrNull()
                val mediaType = intent.getStringExtra("mediaType")
                val commentId = intent.getStringExtra("commentId")
                val seasonNumber = intent.getStringExtra("seasonNumber")?.toIntOrNull()
                val episodeNumber = intent.getStringExtra("episodeNumber")?.toIntOrNull()
                if (mediaId != null && mediaType != null) {
                    when (mediaType) {
                        "movie", "tv" -> rootNavigator.push(
                            MovieDetailScreen(
                                movieId = mediaId,
                                mediaType = mediaType,
                                openComments = true,
                                targetCommentId = commentId,
                                targetSeasonNumber = seasonNumber,
                                targetEpisodeNumber = episodeNumber
                            )
                        )
                    }
                }
            }
            
            deepLinkIntent.value = null
        }
    }
}
