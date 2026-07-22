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
import com.cinetrack.ui.screens.VistiTab
import com.cinetrack.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay

@Composable
fun MainDeepLinkHandler(
    deepLinkIntent: MutableState<Intent?>,
    rootNavigator: Navigator,
    tabNavigator: TabNavigator,
    settingsViewModel: SettingsViewModel,
    searchOverlay: ((Offset?, Long?, String?, Long?, String?) -> Unit)?
) {
    LaunchedEffect(deepLinkIntent.value) {
        val intent = deepLinkIntent.value ?: return@LaunchedEffect
        val uri = intent.data
        val action = intent.action
        
        // 1. LOG DI PARTENZA: Vediamo se l'intento arriva e cosa contiene
        println("FlickTroveDebug: LaunchedEffect intercettato! URI = $uri | Action = $action")

        if (uri != null || action?.startsWith("com.cinetrack.") == true) {
            if (uri != null) {
                // Alziamo a 800ms solo per il test, per escludere al 100% i problemi di caricamento di Voyager
                delay(800) 
            }

            val isCustomScheme = uri != null && uri.scheme == "flicktrove" && (uri.host == "media" || uri.host == "detail")

            val isHttpsAppLink = uri?.scheme == "https" && 
                    uri.host?.lowercase() == "alle-0.github.io" && 
                    uri.path?.contains("open.html", ignoreCase = true) == true

            if (isHttpsAppLink) {
                val type = uri.getQueryParameter("type") ?: "movie"
                val id   = uri.getQueryParameter("id")?.toLongOrNull()
                
                if (id != null) {
                    when (type) {
                        "person" -> rootNavigator.push(PersonDetailScreen(id, null))
                        "collection" -> rootNavigator.push(CollectionDetailScreen(id, null))
                        else -> rootNavigator.push(MovieDetailScreen(id, type))
                    }
                }
            } else if (uri?.scheme == "flicktrove" && uri.host == "auth") {
                // Trakt OAuth callback: flicktrove://auth?code=XXXXX&state=YYYYY
                val code = uri.getQueryParameter("code")
                val returnedState = uri.getQueryParameter("state")
                if (!code.isNullOrEmpty()) {
                    settingsViewModel.exchangeTraktCode(code, returnedState)
                }
            } else if (isCustomScheme) {
                val pathSegments = uri.pathSegments
                val typeFromPath = if (pathSegments.size >= 2) pathSegments[0] else null
                val idFromPath = if (pathSegments.size >= 2) pathSegments[1].toLongOrNull() else null

                val type = uri.getQueryParameter("type") ?: typeFromPath ?: "movie"
                val id = uri.getQueryParameter("id")?.toLongOrNull() ?: idFromPath

                if (id != null) {
                    when (type) {
                        "person" -> rootNavigator.push(PersonDetailScreen(id, null))
                        "collection" -> rootNavigator.push(CollectionDetailScreen(id, null))
                        else -> rootNavigator.push(MovieDetailScreen(id, type))
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
            }
            
            deepLinkIntent.value = null
        }
    }
}
