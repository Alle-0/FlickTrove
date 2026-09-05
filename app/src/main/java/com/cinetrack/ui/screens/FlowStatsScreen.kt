package com.cinetrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import cafe.adriel.voyager.navigator.currentOrThrow
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cinetrack.R
import androidx.compose.ui.res.stringResource
import com.cinetrack.ui.LocalAppPadding
import com.cinetrack.ui.LocalHazeState
import com.cinetrack.ui.components.common.CinematicBackground
import com.cinetrack.ui.viewmodel.FlowViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import com.cinetrack.ui.components.stats.YearSelectionButton
import com.cinetrack.ui.components.stats.YearSelectionModal
import com.cinetrack.ui.viewmodel.TimeRange

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
        val navigator = cafe.adriel.voyager.navigator.LocalNavigator.currentOrThrow.parent
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

        var isYearPickerVisible by remember { mutableStateOf(false) }
        var yearPickerButtonBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }

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
                            top = paddingValues.calculateTopPadding() + WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 100.dp,
                            bottom = paddingValues.calculateBottomPadding() + 120.dp,
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 20.dp)
                    ) {
                        val titleText = when (val range = flowUiState.timeRange) {
                            is com.cinetrack.ui.viewmodel.TimeRange.AllTime -> stringResource(R.string.flow_stats_all_time)
                            is com.cinetrack.ui.viewmodel.TimeRange.Year -> stringResource(R.string.flow_stats_year, range.year.toString())
                        }
                        
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.align(Alignment.CenterStart)
                        )

                        YearSelectionButton(
                            currentRange = flowUiState.timeRange,
                            onToggle = { visible, bounds ->
                                isYearPickerVisible = visible
                                yearPickerButtonBounds = bounds
                            },
                            modifier = Modifier.align(Alignment.CenterEnd)
                        )
                    }

                    // Flow Persona
                    val persona = flowUiState.flowPersona
                    if (persona == null && flowUiState.topVibes.isEmpty() && flowUiState.topMvps.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .padding(top = 100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    painter = androidx.compose.ui.res.painterResource(id = com.cinetrack.R.drawable.ic_stat),
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
                        if (persona != null) {
                            FlowPersonaSection(persona = persona)
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                        
                        // Top Vibes
                        if (flowUiState.topVibes.isNotEmpty()) {
                            TopVibesSection(vibes = flowUiState.topVibes)
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                        
                        // Top MVPs
                        if (flowUiState.topMvps.isNotEmpty()) {
                            TopMvpsSection(
                                mvps = flowUiState.topMvps,
                                onActorClick = { id, profilePath ->
                                    navigator?.push(com.cinetrack.ui.screens.PersonDetailScreen(id, profilePath))
                                }
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
        
        YearSelectionModal(
            isVisible = isYearPickerVisible,
            onDismiss = { isYearPickerVisible = false },
            currentRange = flowUiState.timeRange,
            availableYears = flowUiState.availableYears,
            hazeState = activeHazeState,
            triggerBounds = yearPickerButtonBounds,
            onYearSelected = { year ->
                viewModel?.setTimeRange(TimeRange.Year(year))
                isYearPickerVisible = false
            },
            onAllTimeSelected = {
                viewModel?.setTimeRange(TimeRange.AllTime)
                isYearPickerVisible = false
            }
        )
    }
}

@Composable
fun FlowPersonaSection(persona: com.cinetrack.ui.viewmodel.FlowPersona) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(persona.colorHex).copy(alpha = 0.15f))
            .border(1.dp, Color(persona.colorHex).copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            .padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(persona.colorHex).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(id = persona.iconRes),
                    contentDescription = null,
                    tint = Color(persona.colorHex),
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.your_flow_persona),
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(persona.titleRes),
                color = Color(persona.colorHex),
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(persona.descriptionRes),
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun TopVibesSection(vibes: List<com.cinetrack.ui.viewmodel.VibeStat>) {
    SectionTitle(stringResource(R.string.flow_top_vibes))
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(vibes) { vibe ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (vibe.iconRes != null && vibe.iconRes != 0) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = vibe.iconRes),
                            contentDescription = vibe.vibe,
                            modifier = Modifier.size(32.dp),
                            tint = vibe.colorHex?.let { Color(it) } ?: Color.White
                        )
                    } else {
                        Text(text = vibe.emoji, fontSize = 28.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "x${vibe.count}", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TopMvpsSection(
    mvps: List<com.cinetrack.ui.viewmodel.MvpStat>,
    onActorClick: (Long, String?) -> Unit = { _, _ -> }
) {
    SectionTitle(stringResource(R.string.flow_top_mvps))
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(mvps.take(10)) { mvp ->
            val context = LocalContext.current
            val imageUrl = com.cinetrack.util.buildTmdbImageUrl(
                    mvp.profilePath?.takeIf { it.isNotBlank() && it != "null" },
                    com.cinetrack.util.ImageType.PROFILE,
                    com.cinetrack.util.ImageQuality.MEDIUM
                )
                
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(96.dp)
                    .clickable { onActorClick(mvp.actorId, mvp.profilePath) }
            ) {
                Box {
                    if (imageUrl != null) {
                        coil.compose.SubcomposeAsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = mvp.actorName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                            error = {
                                Box(
                                    modifier = Modifier.fillMaxSize().background(Color.DarkGray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = mvp.actorName.split(" ").filter { it.isNotBlank() }.take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString(""),
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(Color.DarkGray)
                                .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mvp.actorName.split(" ").filter { it.isNotBlank() }.take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString(""),
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    
                    // Count Badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 4.dp, y = 4.dp)
                            .defaultMinSize(minWidth = 24.dp, minHeight = 24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "x${mvp.count}",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = mvp.actorName,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        color = Color.White.copy(alpha = 0.7f),
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = title,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        }
    }
}
