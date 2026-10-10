package com.cinetrack.ui.screens

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.cinetrack.R
import com.cinetrack.ui.LocalAppPadding
import com.cinetrack.ui.LocalHazeState
import com.cinetrack.ui.components.badge.TrophyRoomScreenContent
import com.cinetrack.ui.viewmodel.TrophyRoomViewModel

/**
 * TrophyRoomTab – Tab interno di MainScreen, integrato nella navigazione tramite TabNavigator.
 * Condivide la GlassyTopBar e la GlassyBottomBar fisse di MainScreen, esattamente come StatsTab,
 * FoldersTab e FlowTab.
 *
 * Strategia filtri: il filtro è integrato nella GlassyTopBar di MainScreen (tramite LocalActiveTrophyFilterConfig)
 * e animato con MorphGlassModal dalle coordinate del pulsante filtri della top bar.
 */
object TrophyRoomTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val title = stringResource(id = R.string.dashboard_trophies)
            return remember(title) {
                TabOptions(
                    index = 6u,
                    title = title,
                    icon = null
                )
            }
        }

    @Composable
    override fun Content() {
        val paddingValues = LocalAppPadding.current
        val hazeState = LocalHazeState.current
        val context = LocalContext.current
        val activity = context as? ComponentActivity
        val viewModel = activity?.let { hiltViewModel<TrophyRoomViewModel>(it) }
            ?: hiltViewModel<TrophyRoomViewModel>()
        val trophyItems by viewModel.trophyItems.collectAsStateWithLifecycle()

        TrophyRoomScreenContent(
            trophyItems = trophyItems,
            paddingValues = paddingValues,
            hazeState = hazeState
        )
    }
}

/**
 * Schermata Sala dei Trofei (Trophy Showcase Screen) integrata nello stack Voyager di FlickTrove.
 * Conservata come fallback per eventuali deep link o accessi diretti via parentNavigator.
 * La navigazione primaria avviene tramite TrophyRoomTab in MainScreen.
 */
class TrophyRoomScreen : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        BackHandler {
            navigator.pop()
        }

        val hazeState = LocalHazeState.current
        val paddingValues = LocalAppPadding.current
        val context = LocalContext.current
        val activity = context as? ComponentActivity
        val viewModel = activity?.let { hiltViewModel<TrophyRoomViewModel>(it) }
            ?: hiltViewModel<TrophyRoomViewModel>()
        val trophyItems by viewModel.trophyItems.collectAsStateWithLifecycle()

        TrophyRoomScreenContent(
            trophyItems = trophyItems,
            paddingValues = paddingValues,
            hazeState = hazeState
        )
    }
}
