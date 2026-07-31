package com.cinetrack.ui.screens

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.cinetrack.R
import com.cinetrack.ui.LocalAppPadding
import com.cinetrack.ui.LocalHazeState
import com.cinetrack.ui.components.card.FlowMovieCard
import com.cinetrack.ui.components.common.CinematicBackground
import com.cinetrack.ui.components.shared.MovieActionsWrapper
import com.cinetrack.ui.viewmodel.FlowViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze

object FlowTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val title = "FLOW"
            return remember(title) {
                TabOptions(
                    index = 4u,
                    title = title,
                    icon = null
                )
            }
        }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun Content() {
        val paddingValues = LocalAppPadding.current
        val hazeState = LocalHazeState.current
        val activeHazeState = hazeState ?: remember { HazeState() }
        
        val context = LocalContext.current
        var currentContext = context
        while (currentContext is android.content.ContextWrapper && currentContext !is androidx.activity.ComponentActivity) {
            currentContext = currentContext.baseContext
        }
        val activity = currentContext as? androidx.activity.ComponentActivity
        val viewModel = activity?.let { androidx.hilt.navigation.compose.hiltViewModel<FlowViewModel>(it) }
        val flowUiState by (viewModel?.uiState ?: kotlinx.coroutines.flow.MutableStateFlow(com.cinetrack.ui.viewmodel.FlowUiState())).collectAsStateWithLifecycle()

        val parentNavigator = LocalNavigator.currentOrThrow.parent ?: LocalNavigator.currentOrThrow

        MovieActionsWrapper(
            hazeState = activeHazeState
        ) { _ ->
            Box(modifier = Modifier.fillMaxSize()) {
                CinematicBackground(modifier = Modifier.fillMaxSize())

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .haze(
                            state = activeHazeState,
                            style = HazeStyle(blurRadius = 24.dp, tint = Color.Black.copy(alpha = 0.5f))
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(
                                top = paddingValues.calculateTopPadding() + WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 120.dp,
                                bottom = paddingValues.calculateBottomPadding() + 100.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = flowUiState.movies,
                                key = { it.compositeId }
                            ) { movie ->
                                // Use Box to avoid clipping overlapping MVP card in the grid item
                                Box(modifier = Modifier.animateItem(fadeInSpec = tween(300), placementSpec = tween(300), fadeOutSpec = tween(300))) {
                                    FlowMovieCard(
                                        movie = movie,
                                        cardWidth = 160.dp,
                                        hazeState = activeHazeState,
                                        onPress = { selectedMovie ->
                                            parentNavigator.push(MovieDetailScreen(selectedMovie.id, selectedMovie.mediaType))
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
