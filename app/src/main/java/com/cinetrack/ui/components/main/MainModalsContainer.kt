package com.cinetrack.ui.components.main

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.hilt.getViewModel
import cafe.adriel.voyager.navigator.tab.Tab
import com.cinetrack.ui.FilterModalConfig
import com.cinetrack.ui.LocalActiveFilterConfig
import com.cinetrack.ui.components.dialog.HomeFilterModal
import com.cinetrack.ui.components.stats.YearSelectionModal
import com.cinetrack.ui.screens.DiscoverTab
import com.cinetrack.ui.screens.FolderDetailTab
import com.cinetrack.ui.screens.HomeTab
import com.cinetrack.ui.screens.StatsTab
import com.cinetrack.ui.screens.VistiTab
import com.cinetrack.ui.viewmodel.DiscoverViewModel
import com.cinetrack.ui.viewmodel.HomeViewModel
import com.cinetrack.ui.viewmodel.StatsViewModel
import com.cinetrack.ui.viewmodel.TimeRange
import com.cinetrack.ui.viewmodel.VistiViewModel
import dev.chrisbanes.haze.HazeState

@Composable
fun MainModalsContainer(
    screen: Screen,
    currentTab: Tab,
    activity: ComponentActivity?,
    isFilterModalVisible: Boolean,
    isYearPickerVisible: Boolean,
    filterButtonBounds: Rect?,
    yearPickerButtonBounds: Rect?,
    globalHazeState: HazeState,
    onFilterModalDismiss: () -> Unit,
    onYearPickerDismiss: () -> Unit
) {
    key(currentTab) {
        if (currentTab is HomeTab) {
            val homeViewModel = if (activity != null) {
                hiltViewModel<HomeViewModel>(activity)
            } else {
                with(screen) { getViewModel<HomeViewModel>() }
            }
            val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
            Box(modifier = Modifier.zIndex(70000f)) {
                HomeFilterModal(
                    isVisible = isFilterModalVisible,
                    sortConfig = homeUiState.sortConfig,
                    hazeState = globalHazeState,
                    triggerBounds = filterButtonBounds,
                    onSortConfigChanged = { newConfig -> homeViewModel.updateSortConfig(newConfig); onFilterModalDismiss() },
                    onDismissRequest = onFilterModalDismiss
                )
            }
        } else if (currentTab is VistiTab) {
            val vistiViewModel = if (activity != null) {
                hiltViewModel<VistiViewModel>(activity)
            } else {
                with(screen) { getViewModel<VistiViewModel>() }
            }
            val vistiUiState by vistiViewModel.uiState.collectAsStateWithLifecycle()
            Box(modifier = Modifier.zIndex(70000f)) {
                HomeFilterModal(
                    isVisible = isFilterModalVisible,
                    isVisti = true,
                    sortConfig = vistiUiState.sortConfig,
                    hazeState = globalHazeState,
                    triggerBounds = filterButtonBounds,
                    category = vistiUiState.activeTab,
                    onSortConfigChanged = { newConfig -> vistiViewModel.updateSortConfig(newConfig); onFilterModalDismiss() },
                    onDismissRequest = onFilterModalDismiss
                )
            }
        } else if (currentTab is DiscoverTab) {
            val discoverViewModel = if (activity != null) {
                hiltViewModel<DiscoverViewModel>(activity)
            } else {
                with(screen) { getViewModel<DiscoverViewModel>() }
            }
            val discoverUiState by discoverViewModel.uiState.collectAsStateWithLifecycle()
            Box(modifier = Modifier.zIndex(70000f)) {
                HomeFilterModal(
                    isVisible = isFilterModalVisible,
                    sortConfig = discoverUiState.sortConfig,
                    hazeState = globalHazeState,
                    triggerBounds = filterButtonBounds,
                    showSortBy = false,
                    onSortConfigChanged = { newConfig -> discoverViewModel.updateSortConfig(newConfig); onFilterModalDismiss() },
                    onDismissRequest = onFilterModalDismiss
                )
            }
        } else if (currentTab is StatsTab) {
            val statsViewModel = if (activity != null) {
                hiltViewModel<StatsViewModel>(activity)
            } else {
                with(screen) { getViewModel<StatsViewModel>() }
            }
            val statsUiState by statsViewModel.uiState.collectAsStateWithLifecycle()
            Box(modifier = Modifier.zIndex(70000f)) {
                YearSelectionModal(
                    isVisible = isYearPickerVisible,
                    onDismiss = onYearPickerDismiss,
                    currentRange = statsUiState.timeRange,
                    availableYears = statsUiState.availableYears,
                    hazeState = globalHazeState,
                    triggerBounds = yearPickerButtonBounds,
                    onYearSelected = { year -> statsViewModel.setTimeRange(TimeRange.Year(year)) },
                    onAllTimeSelected = { statsViewModel.setTimeRange(TimeRange.AllTime) }
                )
            }
        } else if (currentTab is FolderDetailTab) {
            val activeFilterConfigState = LocalActiveFilterConfig.current
            val filterConfig = activeFilterConfigState.value
            if (filterConfig != null) {
                Box(modifier = Modifier.zIndex(70000f)) {
                    HomeFilterModal(
                        isVisible = isFilterModalVisible,
                        isVisti = filterConfig.isVisti,
                        sortConfig = filterConfig.sortConfig,
                        hazeState = globalHazeState,
                        triggerBounds = filterButtonBounds,
                        category = filterConfig.category,
                        onSortConfigChanged = { newConfig ->
                            filterConfig.onSortConfigChanged(newConfig)
                            onFilterModalDismiss()
                        },
                        onDismissRequest = onFilterModalDismiss
                    )
                }
            }
        }
    }
}
