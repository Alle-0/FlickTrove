package com.cinetrack.ui.screens

import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.cinetrack.R
import com.cinetrack.ui.LocalAppPadding
import com.cinetrack.ui.LocalFilterRequest
import com.cinetrack.ui.LocalHazeState
import com.cinetrack.ui.components.common.CinematicBackground
import com.cinetrack.ui.components.shared.shimmerEffect
import com.cinetrack.ui.components.stats.FlowPersonaCard
import com.cinetrack.ui.components.stats.TopMvpsSection
import com.cinetrack.ui.components.stats.TopVibesSection
import com.cinetrack.ui.components.stats.YearSelectionButton
import com.cinetrack.ui.viewmodel.FlowUiState
import com.cinetrack.ui.viewmodel.FlowViewModel
import com.cinetrack.ui.viewmodel.TimeRange
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.flow.MutableStateFlow

object FlowStatsTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            return remember {
                TabOptions(
                    index = 10u,
                    title = "Flow Stats",
                    icon = null
                )
            }
        }

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow.parent ?: LocalNavigator.currentOrThrow
        val paddingValues = LocalAppPadding.current
        val hazeState = LocalHazeState.current
        val activeHazeState = hazeState ?: remember { HazeState() }

        val context = LocalContext.current
        var currentContext = context
        while (currentContext is ContextWrapper && currentContext !is ComponentActivity) {
            currentContext = currentContext.baseContext
        }
        val activity = currentContext as? ComponentActivity
        val viewModel = activity?.let { hiltViewModel<FlowViewModel>(it) }
        val flowUiState by (viewModel?.uiState ?: MutableStateFlow(FlowUiState())).collectAsStateWithLifecycle()
        val filterRequest = LocalFilterRequest.current

        Box(
            modifier = Modifier
                .fillMaxSize()
                .haze(state = activeHazeState)
        ) {
            CinematicBackground(modifier = Modifier.fillMaxSize())

            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            top = paddingValues.calculateTopPadding() + WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 90.dp,
                            bottom = paddingValues.calculateBottomPadding() + 120.dp,
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header con Titolo Periodo & Selettore Anno
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val titleText = when (val range = flowUiState.timeRange) {
                            is TimeRange.AllTime -> stringResource(R.string.flow_stats_all_time)
                            is TimeRange.Year -> stringResource(R.string.flow_stats_year, range.year.toString())
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = titleText,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (flowUiState.totalUnfilteredCount > 0) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.flow_stats_evaluated_count, flowUiState.totalUnfilteredCount),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.5f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        YearSelectionButton(
                            currentRange = flowUiState.timeRange,
                            onToggle = { visible, bounds ->
                                if (visible) {
                                    filterRequest?.invoke(bounds)
                                }
                            }
                        )
                    }

                    // Loading vs Stato Vuoto vs Contenuto
                    val persona = flowUiState.flowPersona
                    if (flowUiState.isLoading) {
                        FlowStatsSkeleton()
                    } else if (persona == null && flowUiState.topVibes.isEmpty() && flowUiState.topMvps.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .padding(top = 100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_stat),
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = stringResource(R.string.flow_stats_empty_title),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.flow_stats_empty_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        // 1. Profilo Cinematografico / Persona
                        if (persona != null) {
                            FlowPersonaCard(
                                persona = persona,
                                totalTitles = flowUiState.totalUnfilteredCount,
                                topVibes = flowUiState.topVibes,
                                topMvp = flowUiState.topMvps.firstOrNull()
                            )
                            Spacer(modifier = Modifier.height(28.dp))
                        }

                        // 2. Top Vibes con Spettro Emozionale
                        if (flowUiState.topVibes.isNotEmpty()) {
                            TopVibesSection(vibes = flowUiState.topVibes)
                            Spacer(modifier = Modifier.height(28.dp))
                        }

                        // 3. Top MVPs (Attori preferiti)
                        if (flowUiState.topMvps.isNotEmpty()) {
                            TopMvpsSection(
                                mvps = flowUiState.topMvps,
                                onActorClick = { id, profilePath ->
                                    navigator.push(PersonDetailScreen(id, profilePath))
                                }
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlowStatsSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        // 1. Persona Card Placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .clip(RoundedCornerShape(24.dp))
                .shimmerEffect()
        )

        // 2. Vibes Section Header & Chart Placeholder
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .shimmerEffect()
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .shimmerEffect()
            )
        }

        // 3. MVPs Section Placeholder
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .shimmerEffect()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(110.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .shimmerEffect()
                    )
                }
            }
        }
    }
}
