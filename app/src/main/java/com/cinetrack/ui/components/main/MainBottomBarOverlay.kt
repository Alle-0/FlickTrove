package com.cinetrack.ui.components.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import cafe.adriel.voyager.navigator.tab.Tab
import com.cinetrack.ui.components.navigation.GlassyBottomBar
import com.cinetrack.ui.screens.DiscoverTab
import com.cinetrack.ui.screens.FolderDetailTab
import com.cinetrack.ui.screens.FoldersTab
import com.cinetrack.ui.screens.HomeTab
import com.cinetrack.ui.screens.NewsTab
import com.cinetrack.ui.screens.RecommendationsTab
import com.cinetrack.ui.screens.SettingsTab
import com.cinetrack.ui.screens.StatsTab
import com.cinetrack.ui.screens.VistiTab
import dev.chrisbanes.haze.HazeState

fun isPrimaryMainTab(currentTab: Tab): Boolean {
    return currentTab is HomeTab ||
            currentTab is VistiTab ||
            currentTab is StatsTab ||
            currentTab is DiscoverTab ||
            currentTab is FoldersTab ||
            currentTab is FolderDetailTab ||
            currentTab is SettingsTab ||
            currentTab is RecommendationsTab ||
            currentTab is NewsTab
}

@Composable
fun BoxScope.MainBottomBarOverlay(
    currentTab: Tab,
    contentHazeState: HazeState,
    isSettingsDialogOpen: Boolean,
    onDimmedAreaClick: () -> Unit,
    onNavigate: (String) -> Unit
) {
    if (isPrimaryMainTab(currentTab)) {
        Box(modifier = Modifier.align(Alignment.BottomCenter).zIndex(50f)) {
            GlassyBottomBar(
                hazeState = contentHazeState,
                isDimmed = isSettingsDialogOpen,
                onDimmedAreaClick = onDimmedAreaClick,
                selectedRoute = when (currentTab) {
                    is HomeTab -> "index"
                    is VistiTab -> "visti"
                    is StatsTab -> "stats"
                    else -> null
                },
                onNavigate = onNavigate
            )
        }
    }
}
