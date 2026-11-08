package com.cinetrack.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowDown
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.cinetrack.LocalAdvancedVisualEffects
import com.cinetrack.R
import com.cinetrack.ui.LocalAppPadding
import com.cinetrack.ui.LocalHazeState
import com.cinetrack.ui.components.common.CinematicBackground
import com.cinetrack.ui.components.stats.CountingText
import com.cinetrack.ui.components.stats.YearSelectionButton
import com.cinetrack.ui.components.stats.statsCard
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.viewmodel.FlowPersona
import com.cinetrack.ui.viewmodel.FlowViewModel
import com.cinetrack.ui.viewmodel.MvpStat
import com.cinetrack.ui.viewmodel.TimeRange
import com.cinetrack.ui.viewmodel.VibeStat
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

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
        val navigator = cafe.adriel.voyager.navigator.LocalNavigator.currentOrThrow.parent ?: cafe.adriel.voyager.navigator.LocalNavigator.currentOrThrow
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
        val filterRequest = com.cinetrack.ui.LocalFilterRequest.current

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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        val titleText = when (val range = flowUiState.timeRange) {
                            is TimeRange.AllTime -> stringResource(R.string.flow_stats_all_time)
                            is TimeRange.Year -> stringResource(R.string.flow_stats_year, range.year.toString())
                        }
                        
                        Column(modifier = Modifier.align(Alignment.CenterStart)) {
                            Text(
                                text = titleText,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            if (flowUiState.totalUnfilteredCount > 0) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.flow_stats_evaluated_count, flowUiState.totalUnfilteredCount),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }

                        YearSelectionButton(
                            currentRange = flowUiState.timeRange,
                            onToggle = { visible, bounds ->
                                if (visible) {
                                    filterRequest?.invoke(bounds)
                                }
                            },
                            modifier = Modifier.align(Alignment.CenterEnd)
                        )
                    }

                    // Stato Vuoto vs Contenuto
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
                                    navigator.push(com.cinetrack.ui.screens.PersonDetailScreen(id, profilePath))
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

/**
 * Card principale del profilo / persona cinematografica
 */
@Composable
fun FlowPersonaCard(
    persona: FlowPersona,
    totalTitles: Int,
    topVibes: List<VibeStat>,
    topMvp: MvpStat? = null
) {
    val context = LocalContext.current
    val totalVibesCount = remember(topVibes) { topVibes.sumOf { it.count } }
    val topVibeStat = topVibes.firstOrNull()
    val topVibeCount = topVibeStat?.count ?: 0
    val dominancePercent = if (totalVibesCount > 0) (topVibeCount * 100) / totalVibesCount else 0
    val personaColor = Color(persona.colorHex)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        personaColor.copy(alpha = 0.24f),
                        personaColor.copy(alpha = 0.16f)
                    )
                )
            )
            .border(
                1.dp,
                personaColor.copy(alpha = 0.35f),
                RoundedCornerShape(24.dp)
            )
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Riga Avatar e Statistiche
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(personaColor.copy(alpha = 0.15f))
                        .border(1.dp, personaColor.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = persona.iconRes),
                        contentDescription = null,
                        tint = personaColor,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "$topVibeCount film • $dominancePercent%",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Nome Stile / Persona
            Text(
                text = stringResource(persona.titleRes),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Descrizione
            Text(
                text = stringResource(persona.descriptionRes),
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 13.5.sp,
                lineHeight = 19.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.08f),
                thickness = 1.dp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Footer con 2 metriche reali e pulsante Condividi
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val topVibeName = topVibeStat?.let {
                    it.labelRes?.let { res -> stringResource(res) } ?: it.vibe
                } ?: "-"

                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.flow_stats_insight_vibe),
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 10.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = topVibeName,
                            color = personaColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (topMvp != null) {
                        Column(modifier = Modifier.widthIn(max = 110.dp)) {
                            Text(
                                text = "Top MVP",
                                color = Color.White.copy(alpha = 0.45f),
                                fontSize = 10.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = topMvp.actorName,
                                color = Color(0xFFFFD700),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Column {
                        Text(
                            text = stringResource(R.string.flow_stats_insight_titles),
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 10.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$totalTitles",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                val titleString = stringResource(persona.titleRes)
                Row(
                    modifier = Modifier
                        .bounceClick { shareFlowPersona(context, titleString) }
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_share),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = stringResource(R.string.card_share),
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Sezione Top Vibes: Donut Chart interattivo come nelle statistiche generali,
 * con alone luminoso organico, fette cliccabili, info al centro ed etichette visibili.
 */
@Composable
private fun VibeLegendItem(
    vibe: VibeStat,
    percentage: Int,
    color: Color,
    isSelected: Boolean,
    isDimmed: Boolean,
    onClick: () -> Unit
) {
    val bgAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.12f else 0.02f,
        label = "vibe_bgAlpha"
    )
    val vibeName = vibe.labelRes?.let { stringResource(it) }
        ?: vibe.vibe.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .bounceClick(onClick = onClick)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = bgAlpha))
            .padding(start = 8.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val iconRes = vibe.iconRes ?: if (vibe.vibe == "OTHER") R.drawable.ic_sparkle else null
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(color.copy(alpha = if (isSelected) 0.25f else 0.14f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (iconRes != null && iconRes != 0) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = color
                )
            } else {
                Text(
                    text = vibe.emoji,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Text(
            text = vibeName,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                color = if (isSelected) color else Color.White.copy(alpha = if (isDimmed) 0.35f else 0.9f)
            ),
            modifier = Modifier.weight(1f)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${vibe.count}",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = if (isSelected) color else Color.White.copy(alpha = if (isDimmed) 0.35f else 0.8f)
                )
            )
            Spacer(Modifier.width(8.dp))
            val percentageText = if (percentage == 0 && vibe.count > 0) "<1%" else "$percentage%"
            Text(
                text = percentageText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = if (isSelected) 0.5f else 0.25f)
                )
            )
        }
    }
}

/**
 * Sezione Top Vibes: Donut Chart con lista verticale identica alla schermata statistiche normali (GenreDistributionSection)
 */
@Composable
fun TopVibesSection(vibes: List<VibeStat>) {
    if (vibes.isEmpty()) return

    val totalCount = remember(vibes) { vibes.sumOf { it.count } }
    val textMeasurer = rememberTextMeasurer()

    val otherVibeLabel = stringResource(R.string.stats_genre_other)
    val processedVibes = remember(vibes, otherVibeLabel) {
        if (vibes.size <= 8) {
            vibes
        } else {
            val mainVibes = vibes.take(7)
            val othersCount = vibes.drop(7).sumOf { it.count }
            mainVibes + VibeStat(
                vibe = "OTHER",
                emoji = "✨",
                count = othersCount,
                iconRes = R.drawable.ic_sparkle,
                colorHex = 0xFFFF9800,
                labelRes = R.string.stats_genre_other
            )
        }
    }

    val context = LocalContext.current
    val density = LocalDensity.current
    val vibeDrawables = remember(processedVibes) {
        processedVibes.associate { vibe ->
            val resId = vibe.iconRes ?: if (vibe.vibe == "OTHER") R.drawable.ic_sparkle else null
            vibe.vibe to (resId?.let { ContextCompat.getDrawable(context, it)?.mutate() })
        }
    }

    var selectedVibeCode by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }

    val chartSelectedIndex = remember(selectedVibeCode, processedVibes) {
        if (selectedVibeCode == null) {
            -1
        } else {
            val idx = processedVibes.indexOfFirst { it.vibe == selectedVibeCode }
            if (idx != -1) idx else processedVibes.lastIndex
        }
    }

    val animOffsets = processedVibes.mapIndexed { idx, _ ->
        animateFloatAsState(
            targetValue = if (chartSelectedIndex == idx) 1f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "vibe_offset_$idx"
        )
    }

    val initialAnim by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "vibe_initialAnim"
    )

    val defaultAccentColor = vibes.firstOrNull()?.colorHex?.let { Color(it) } ?: MaterialTheme.colorScheme.primary
    val animatedHaloColor by animateColorAsState(
        targetValue = if (selectedVibeCode != null && chartSelectedIndex != -1) {
            processedVibes[chartSelectedIndex].colorHex?.let { Color(it) } ?: defaultAccentColor
        } else {
            defaultAccentColor
        },
        animationSpec = tween(500),
        label = "vibe_haloColor"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .statsCard(RoundedCornerShape(32.dp))
            .animateContentSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Donut Chart Box
            Box(
                modifier = Modifier
                    .size(310.dp)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                val strokeWidthPx = with(density) { 26.dp.toPx() }
                val donutRadius = with(density) { 112.dp.toPx() }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(processedVibes, initialAnim) {
                            val canvasSize = size
                            detectTapGestures { offset ->
                                val canvasWidth = canvasSize.width
                                val canvasHeight = canvasSize.height
                                val centerX = canvasWidth / 2f
                                val centerY = canvasHeight / 2f
                                val dx = offset.x - centerX
                                val dy = offset.y - centerY
                                val distance = sqrt(dx * dx + dy * dy)

                                val outerRadius = donutRadius
                                val innerRadius = outerRadius - strokeWidthPx

                                if (distance in innerRadius..outerRadius) {
                                    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                    if (angle < 0) angle += 360f
                                    val normalizedAngle = (angle + 90f) % 360f

                                    var currentAngle = 0f
                                    var foundIndex = -1
                                    for (i in processedVibes.indices) {
                                        val sweep = (processedVibes[i].count.toFloat() / totalCount) * 360f
                                        if (normalizedAngle >= currentAngle && normalizedAngle < currentAngle + sweep) {
                                            foundIndex = i
                                            break
                                        }
                                        currentAngle += sweep
                                    }

                                    if (foundIndex != -1) {
                                        val clickedCode = processedVibes[foundIndex].vibe
                                        selectedVibeCode = if (selectedVibeCode == clickedCode) null else clickedCode
                                    }
                                } else {
                                    selectedVibeCode = null
                                }
                            }
                        }
                ) {
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    var currentStartAngle = -90f

                    processedVibes.forEachIndexed { index, vibe ->
                        val sweepAngle = (vibe.count.toFloat() / totalCount) * 360f * initialAnim
                        val isSelected = (chartSelectedIndex == index)
                        val selectionOffset = animOffsets[index].value * 8.dp.toPx()

                        val midAngle = currentStartAngle + sweepAngle / 2f
                        val rad = Math.toRadians(midAngle.toDouble())
                        val cosVal = cos(rad).toFloat()
                        val sinVal = sin(rad).toFloat()
                        val offsetX = cosVal * selectionOffset
                        val offsetY = sinVal * selectionOffset

                        val currentStrokeWidth = if (isSelected) strokeWidthPx + 4.dp.toPx() else strokeWidthPx
                        val color = vibe.colorHex?.let { Color(it) } ?: Color.White
                        val alpha = if (chartSelectedIndex == -1 || isSelected) 1f else 0.25f

                        val pathRadius = donutRadius - strokeWidthPx / 2f
                        val arcSize = Size(pathRadius * 2f, pathRadius * 2f)
                        val arcTopLeft = Offset(centerX - pathRadius + offsetX, centerY - pathRadius + offsetY)

                        if (isSelected) {
                            drawArc(
                                color = color.copy(alpha = 0.25f),
                                startAngle = currentStartAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                topLeft = arcTopLeft,
                                size = arcSize,
                                style = Stroke(width = currentStrokeWidth + 8.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        drawArc(
                            color = color.copy(alpha = alpha),
                            startAngle = currentStartAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = arcTopLeft,
                            size = arcSize,
                            style = Stroke(width = currentStrokeWidth, cap = if (isSelected) StrokeCap.Round else StrokeCap.Butt)
                        )

                        // Etichette esterne con icona e percentuale
                        val percentage = if (totalCount > 0) (vibe.count.toFloat() / totalCount * 100).roundToInt() else 0
                        val percentageText = if (percentage == 0 && vibe.count > 0) "<1%" else "$percentage%"
                        if (sweepAngle > 8f && vibe.count > 0) {
                            val textRadius = donutRadius + 11.dp.toPx() + selectionOffset
                            val textX = centerX + cosVal * textRadius
                            val textY = centerY + sinVal * textRadius

                            val drawable = vibeDrawables[vibe.vibe]
                            val iconSizePx = with(density) { 15.dp.toPx() }
                            val spacingPx = with(density) { 3.dp.toPx() }

                            val percentageLayoutResult = textMeasurer.measure(
                                text = percentageText,
                                style = TextStyle(
                                    color = color.copy(alpha = alpha),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold
                                )
                            )

                            val totalWidth = iconSizePx + spacingPx + percentageLayoutResult.size.width
                            val totalHeight = maxOf(iconSizePx, percentageLayoutResult.size.height.toFloat())

                            val x = if (cosVal > 0.2f) textX else if (cosVal < -0.2f) textX - totalWidth else textX - totalWidth / 2f
                            val y = if (sinVal > 0.2f) textY else if (sinVal < -0.2f) textY - totalHeight else textY - totalHeight / 2f

                            val lineStartX = centerX + cosVal * (donutRadius + 2.dp.toPx() + selectionOffset)
                            val lineStartY = centerY + sinVal * (donutRadius + 2.dp.toPx() + selectionOffset)
                            val lineEndX = centerX + cosVal * (donutRadius + 7.dp.toPx() + selectionOffset)
                            val lineEndY = centerY + sinVal * (donutRadius + 7.dp.toPx() + selectionOffset)

                            drawLine(
                                color = color.copy(alpha = alpha * 0.4f),
                                start = Offset(lineStartX, lineStartY),
                                end = Offset(lineEndX, lineEndY),
                                strokeWidth = 1.dp.toPx()
                            )

                            if (drawable != null) {
                                val iconLeft = x
                                val iconTop = y + (totalHeight - iconSizePx) / 2f
                                val iconRight = iconLeft + iconSizePx
                                val iconBottom = iconTop + iconSizePx

                                drawIntoCanvas { nativeCanvas ->
                                    drawable.setBounds(iconLeft.toInt(), iconTop.toInt(), iconRight.toInt(), iconBottom.toInt())
                                    drawable.setTint(color.copy(alpha = alpha).toArgb())
                                    drawable.draw(nativeCanvas.nativeCanvas)
                                }

                                val textXPos = iconRight + spacingPx
                                val textYPos = y + (totalHeight - percentageLayoutResult.size.height) / 2f

                                drawText(
                                    textLayoutResult = percentageLayoutResult,
                                    topLeft = Offset(textXPos, textYPos)
                                )
                            } else {
                                val fallbackLayoutResult = textMeasurer.measure(
                                    text = "${vibe.emoji} $percentageText",
                                    style = TextStyle(
                                        color = color.copy(alpha = alpha),
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold
                                    )
                                )
                                drawText(
                                    textLayoutResult = fallbackLayoutResult,
                                    topLeft = Offset(x, y)
                                )
                            }
                        }

                        currentStartAngle += sweepAngle
                    }
                }

                // Breathing Halo neon interno
                val advancedEffectsEnabled = LocalAdvancedVisualEffects.current
                val (haloScale, haloAlpha) = if (advancedEffectsEnabled) {
                    val infiniteTransition = rememberInfiniteTransition(label = "vibe_breathing")
                    val breathingScale by infiniteTransition.animateFloat(
                        initialValue = 0.96f,
                        targetValue = 1.04f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "vibe_scale"
                    )
                    val breathingAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.15f,
                        targetValue = 0.35f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "vibe_alpha"
                    )
                    breathingScale to breathingAlpha
                } else {
                    1f to 0.25f
                }

                Box(
                    modifier = Modifier
                        .size(165.dp)
                        .graphicsLayer {
                            scaleX = haloScale
                            scaleY = haloScale
                            alpha = haloAlpha
                        }
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    animatedHaloColor,
                                    animatedHaloColor.copy(alpha = 0f)
                                )
                            ),
                            shape = CircleShape
                        )
                )

                // Centro Frosted Glass
                Box(
                    modifier = Modifier
                        .size(158.dp)
                        .background(Color(0xFF0F0F1A).copy(alpha = 0.75f), CircleShape)
                        .border(
                            width = 1.5.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    animatedHaloColor.copy(alpha = 0.5f),
                                    Color.White.copy(alpha = 0.05f)
                                )
                            ),
                            shape = CircleShape
                        )
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        val activeCode = selectedVibeCode
                        if (activeCode != null) {
                            val targetVibe = processedVibes.firstOrNull { it.vibe == activeCode }
                            val count = targetVibe?.count ?: 0
                            val percentage = if (totalCount > 0) ((count.toFloat() / totalCount) * 100).roundToInt() else 0
                            val percentageText = if (percentage == 0 && count > 0) "<1%" else "$percentage%"
                            val countColor = targetVibe?.colorHex?.let { Color(it) } ?: MaterialTheme.colorScheme.primary
                            val vibeDisplayName = targetVibe?.labelRes?.let { stringResource(it) }
                                ?: targetVibe?.vibe?.lowercase()?.replace("_", " ")?.replaceFirstChar { it.uppercase() }
                                ?: activeCode

                            val centerIconRes = targetVibe?.iconRes ?: if (targetVibe?.vibe == "OTHER") R.drawable.ic_sparkle else null
                            if (centerIconRes != null && centerIconRes != 0) {
                                Icon(
                                    painter = painterResource(id = centerIconRes),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = countColor
                                )
                                Spacer(Modifier.height(3.dp))
                            }

                            Text(
                                text = vibeDisplayName.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color.White.copy(alpha = 0.5f),
                                    letterSpacing = 1.5.sp,
                                    fontSize = 10.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            CountingText(
                                target = count,
                                color = countColor,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = if (count == 1) stringResource(R.string.stats_title_singular) else stringResource(R.string.stats_title_plural),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = countColor.copy(alpha = 0.7f),
                                    fontSize = 9.sp,
                                    letterSpacing = 1.sp
                                )
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.stats_of_total, percentageText),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 9.sp
                                )
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.flow_top_vibes).uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color.White.copy(alpha = 0.5f),
                                    letterSpacing = 1.5.sp,
                                    fontSize = 10.sp
                                )
                            )
                            Spacer(Modifier.height(2.dp))
                            CountingText(
                                target = totalCount,
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.stats_watched),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 9.sp,
                                    letterSpacing = 1.sp
                                )
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.stats_tap_slice),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    fontSize = 8.sp,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Lista Verticale di LegendItem con fading scroll (identica a GenreDistributionSection)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = if (expanded) 320.dp else 200.dp)
            ) {
                val scrollState = rememberScrollState()
                val nestedScrollConnection = remember {
                    object : NestedScrollConnection {
                        override fun onPostScroll(
                            consumed: Offset,
                            available: Offset,
                            source: NestedScrollSource
                        ): Offset = available
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                        .drawWithContent {
                            drawContent()
                            if (scrollState.value > 0) {
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        0f to Color.Transparent,
                                        1f to Color.Black,
                                        startY = 0f,
                                        endY = size.height * 0.15f
                                    ),
                                    blendMode = BlendMode.DstIn
                                )
                            }
                            if (scrollState.value < scrollState.maxValue) {
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        0f to Color.Black,
                                        1f to Color.Transparent,
                                        startY = size.height * 0.75f,
                                        endY = size.height
                                    ),
                                    blendMode = BlendMode.DstIn
                                )
                            }
                        }
                        .nestedScroll(nestedScrollConnection)
                        .verticalScroll(scrollState)
                        .padding(vertical = 12.dp)
                ) {
                    val displayList = if (expanded) vibes else vibes.take(7)

                    displayList.forEachIndexed { index, vibe ->
                        val percentage = if (totalCount > 0) ((vibe.count.toFloat() / totalCount) * 100).roundToInt() else 0
                        val color = vibe.colorHex?.let { Color(it) } ?: Color.White

                        val isItemSelected = (selectedVibeCode == vibe.vibe)
                        val isDimmed = selectedVibeCode != null && !isItemSelected

                        VibeLegendItem(
                            vibe = vibe,
                            percentage = percentage,
                            color = color,
                            isSelected = isItemSelected,
                            isDimmed = isDimmed,
                            onClick = {
                                selectedVibeCode = if (isItemSelected) null else vibe.vibe
                            }
                        )
                        if (index < displayList.lastIndex) {
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }

                if (expanded) {
                    val scrollFraction by remember {
                        derivedStateOf { if (scrollState.maxValue > 0) scrollState.value.toFloat() / scrollState.maxValue else 0f }
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 10.dp, bottom = 10.dp, end = 4.dp)
                            .fillMaxHeight()
                            .width(3.dp)
                            .background(Color.White.copy(alpha = 0.05f), CircleShape)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.2f)
                                .offset(y = (240 * scrollFraction).dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape)
                        )
                    }
                }
            }

            if (vibes.size > 5) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .bounceClick(onClick = { expanded = !expanded })
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (expanded) stringResource(R.string.stats_see_less) else stringResource(R.string.stats_see_all),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Icon(
                                imageVector = if (expanded) Icons.Rounded.KeyboardDoubleArrowUp else Icons.Rounded.KeyboardDoubleArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(start = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Sezione Top MVPs senza box scuri e con badge numerici circolari perfetti
 */
@Composable
fun TopMvpsSection(
    mvps: List<MvpStat>,
    onActorClick: (Long, String?) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current

    Text(
        text = stringResource(R.string.flow_top_mvps),
        color = Color.White.copy(alpha = 0.9f),
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    )

    Spacer(modifier = Modifier.height(8.dp))

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(mvps.take(10), key = { _, mvp -> mvp.actorId }) { index, mvp ->
            val imageUrl = com.cinetrack.util.buildTmdbImageUrl(
                mvp.profilePath?.takeIf { it.isNotBlank() && it != "null" },
                com.cinetrack.util.ImageType.PROFILE,
                com.cinetrack.util.ImageQuality.MEDIUM
            )

            val rankColor = when (index) {
                0 -> Color(0xFFFFD700) // Oro
                1 -> Color(0xFFE0E0E0) // Argento
                2 -> Color(0xFFCD7F32) // Bronzo
                else -> Color.White.copy(alpha = 0.3f)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(96.dp)
                    .bounceClick { onActorClick(mvp.actorId, mvp.profilePath) }
            ) {
                Box {
                    if (imageUrl != null) {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = mvp.actorName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .border(2.dp, rankColor, CircleShape),
                            error = {
                                InitialsAvatar(name = mvp.actorName)
                            }
                        )
                    } else {
                        InitialsAvatar(name = mvp.actorName)
                    }

                    // Badge Posizione (Cerchio Perfetto 1:1) per la top 3
                    if (index < 3) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(x = (-2).dp, y = (-2).dp)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(rankColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    // Badge Conteggio xN (Cerchio Perfetto 1:1)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 4.dp, y = 4.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (index == 0) Color(0xFFFFD700) else MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "x${mvp.count}",
                            color = if (index == 0) Color.Black else MaterialTheme.colorScheme.onPrimary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Black
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
                    lineHeight = 14.sp,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                val titlesText = if (mvp.movieTitles.isNotEmpty()) {
                    mvp.movieTitles.take(2).joinToString(", ")
                } else {
                    ""
                }
                if (titlesText.isNotBlank()) {
                    Text(
                        text = titlesText,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InitialsAvatar(name: String) {
    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(Color.DarkGray)
            .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .mapNotNull { it.firstOrNull()?.uppercase() }
                .joinToString(""),
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Condivisione nativa tramite Android Share Intent
 */
private fun shareFlowPersona(context: Context, personaTitle: String) {
    try {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(
                Intent.EXTRA_TEXT,
                context.getString(R.string.flow_stats_share_text, personaTitle)
            )
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, null)
        context.startActivity(shareIntent)
    } catch (_: Exception) {
        // Fallback silenzioso
    }
}
