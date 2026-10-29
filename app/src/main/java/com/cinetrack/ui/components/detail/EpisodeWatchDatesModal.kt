package com.cinetrack.ui.components.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.data.local.entities.WatchHistoryEntity
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.components.shared.FlickTroveDatePickerModal
import com.cinetrack.ui.components.shared.FlickTroveModal
import com.cinetrack.ui.components.shared.ModalCloseButton
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.verticalFadingEdges
import dev.chrisbanes.haze.HazeState
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private data class TargetEdit(
    val seasonNumber: Int,
    val episodeNumber: Int? = null, // null means whole season
    val currentLocalDate: LocalDate
)

@Composable
fun EpisodeWatchDatesModal(
    movie: Movie,
    history: WatchHistoryEntity,
    accentColor: Color,
    onDismiss: () -> Unit,
    onUpdateEpisodeDate: (seasonNumber: Int, episodeNumber: Int, newDateIso: String) -> Unit,
    onUpdateSeasonDate: (seasonNumber: Int, episodeNumbers: List<Int>, newDateIso: String) -> Unit,
    sessionLabel: String? = null,
    hazeState: HazeState? = null
) {
    var activeEdit by remember { mutableStateOf<TargetEdit?>(null) }
    var collapsedSeasons by remember { mutableStateOf<Set<Int>>(emptySet()) }

    val modalVisibilityAlpha by animateFloatAsState(
        targetValue = if (activeEdit != null) 0f else 1f,
        animationSpec = tween(durationMillis = 300),
        label = "modalVisibilityAlpha"
    )

    // Parse the episodes in this session
    val sessionEpisodes: Map<String, List<Int>> = remember(history.historicalEpisodes, movie.watchedEpisodes) {
        if (!history.historicalEpisodes.isNullOrBlank()) {
            try {
                Json.decodeFromString<Map<String, List<Int>>>(history.historicalEpisodes)
            } catch (e: Exception) {
                movie.watchedEpisodes ?: emptyMap()
            }
        } else {
            movie.watchedEpisodes ?: emptyMap()
        }
    }

    // Parse the custom episode dates map
    val episodeDatesMap: Map<String, String> = remember(history.episodeDates) {
        if (!history.episodeDates.isNullOrBlank()) {
            try {
                Json.decodeFromString<Map<String, String>>(history.episodeDates)
            } catch (e: Exception) {
                emptyMap()
            }
        } else {
            emptyMap()
        }
    }

    val validSeasons = remember(sessionEpisodes) {
        sessionEpisodes.filterKeys { it != "0" }
            .mapNotNull { entry ->
                val sNum = entry.key.toIntOrNull() ?: return@mapNotNull null
                val sortedEps = entry.value.sorted()
                if (sortedEps.isNotEmpty()) sNum to sortedEps else null
            }
            .sortedBy { it.first }
    }

    Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = modalVisibilityAlpha }) {
        FlickTroveModal(
            onDismissRequest = onDismiss,
            hazeState = hazeState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = stringResource(id = R.string.watch_history_episode_dates_title),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        val subtitle = remember(movie.name, movie.title, sessionLabel) {
                            val title = movie.name ?: movie.title ?: ""
                            if (!sessionLabel.isNullOrBlank()) {
                                if (title.isNotBlank()) "$title • $sessionLabel" else sessionLabel
                            } else {
                                title
                            }
                        }
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    ModalCloseButton(onClick = onDismiss)
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (validSeasons.isEmpty()) {
                    Text(
                        text = stringResource(id = R.string.watch_history_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(vertical = 32.dp)
                    )
                } else {
                    val lazyListState = rememberLazyListState()
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 440.dp)
                            .verticalFadingEdges(
                                lazyListState = lazyListState,
                                topEdgeHeight = 20.dp,
                                bottomEdgeHeight = 20.dp
                            ),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        validSeasons.forEachIndexed { sIndex, (seasonNum, eps) ->
                            val isCollapsed = seasonNum in collapsedSeasons

                            item(key = "season_$seasonNum") {
                                val chevronRotation by animateFloatAsState(
                                    targetValue = if (isCollapsed) 0f else 90f,
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                    label = "chevronRotation_$seasonNum"
                                )
                                val connectorAlpha by animateFloatAsState(
                                    targetValue = if (isCollapsed) 0f else 0.35f,
                                    animationSpec = tween(durationMillis = 180),
                                    label = "connectorAlpha_$seasonNum"
                                )

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                top = if (sIndex == 0) 2.dp else 16.dp,
                                                bottom = 6.dp
                                            ),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Clickable Season Title to toggle collapse/expand
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .weight(1f, fill = false)
                                                .bounceClick(scaleDown = 0.96f) {
                                                    collapsedSeasons = if (isCollapsed) {
                                                        collapsedSeasons - seasonNum
                                                    } else {
                                                        collapsedSeasons + seasonNum
                                                    }
                                                }
                                        ) {
                                            Canvas(
                                                modifier = Modifier
                                                    .width(20.dp)
                                                    .height(26.dp)
                                            ) {
                                                val cx = 7.dp.toPx()
                                                val cy = size.height / 2
                                                val strokeW = 1.5.dp.toPx()

                                                // Season indicator dot
                                                drawCircle(
                                                    color = accentColor,
                                                    radius = 4.5.dp.toPx(),
                                                    center = Offset(cx, cy)
                                                )

                                                // Vertical connector line running down towards episodes (smooth fade with expand/collapse)
                                                if (eps.isNotEmpty() && connectorAlpha > 0.01f) {
                                                    drawLine(
                                                        color = accentColor.copy(alpha = connectorAlpha),
                                                        start = Offset(cx, cy + 4.5.dp.toPx()),
                                                        end = Offset(cx, size.height),
                                                        strokeWidth = strokeW,
                                                        cap = StrokeCap.Round
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(4.dp))

                                            Text(
                                                text = "${movie.seasons?.find { it.seasonNumber == seasonNum }?.name?.takeIf { it.isNotBlank() } ?: stringResource(id = R.string.episodes_season_n, seasonNum)} (${eps.size} ep)",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = accentColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            Spacer(modifier = Modifier.width(2.dp))

                                            Icon(
                                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_right),
                                                contentDescription = if (isCollapsed) "Expand" else "Collapse",
                                                tint = accentColor.copy(alpha = 0.75f),
                                                modifier = Modifier
                                                    .size(13.dp)
                                                    .graphicsLayer { rotationZ = chevronRotation }
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // Quick Action: Apply to all episodes in this season
                                        Row(
                                            modifier = Modifier
                                                .bounceClick(scaleDown = 0.95f) {
                                                    val firstDate = eps.firstOrNull()?.let { ep ->
                                                        parseWatchDateToLocalDate(episodeDatesMap["${seasonNum}_$ep"] ?: history.watchedAt)
                                                    } ?: LocalDate.now()
                                                    activeEdit = TargetEdit(seasonNumber = seasonNum, episodeNumber = null, currentLocalDate = firstDate)
                                                }
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.White.copy(alpha = 0.08f))
                                                .padding(horizontal = 8.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_calendario),
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = 0.8f),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = stringResource(id = R.string.watch_history_apply_to_season),
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                color = Color.White.copy(alpha = 0.8f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    // Dynamic smooth accordion expand/collapse animation
                                    AnimatedVisibility(
                                        visible = !isCollapsed,
                                        enter = expandVertically(
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            ),
                                            expandFrom = Alignment.Top
                                        ) + fadeIn(animationSpec = tween(180)),
                                        exit = shrinkVertically(
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            ),
                                            shrinkTowards = Alignment.Top
                                        ) + fadeOut(animationSpec = tween(120))
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            eps.forEachIndexed { epIndex, ep ->
                                                val isLast = epIndex == eps.size - 1
                                                val rawDate = episodeDatesMap["${seasonNum}_$ep"] ?: history.watchedAt
                                                val displayDate = formatDateToLocalDisplay(rawDate)
                                                val parsedDate = parseWatchDateToLocalDate(rawDate) ?: LocalDate.now()

                                                val episodeTitle = movie.seasons
                                                    ?.find { it.seasonNumber == seasonNum }
                                                    ?.episodes
                                                    ?.find { it.episodeNumber == ep }
                                                    ?.name
                                                    ?.takeIf { it.isNotBlank() }

                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(IntrinsicSize.Min)
                                                        .padding(bottom = if (isLast) 2.dp else 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Thread hierarchy connector line with smooth rounded branch
                                                    Canvas(
                                                        modifier = Modifier
                                                            .width(20.dp)
                                                            .fillMaxHeight()
                                                    ) {
                                                        val cx = 7.dp.toPx()
                                                        val cy = size.height / 2
                                                        val strokeW = 1.5.dp.toPx()
                                                        val lineColor = accentColor.copy(alpha = 0.35f)
                                                        val cornerR = 8.dp.toPx().coerceAtMost(cy)

                                                        val path = Path().apply {
                                                            moveTo(cx, 0f)
                                                            if (isLast) {
                                                                // Smooth rounded bend curving into the episode card
                                                                lineTo(cx, cy - cornerR)
                                                                quadraticTo(cx, cy, cx + cornerR, cy)
                                                                lineTo(size.width, cy)
                                                            } else {
                                                                // Main vertical spine continues straight down to next episode
                                                                lineTo(cx, size.height)
                                                                // Branch curves smoothly into this episode card
                                                                moveTo(cx, cy - cornerR)
                                                                quadraticTo(cx, cy, cx + cornerR, cy)
                                                                lineTo(size.width, cy)
                                                            }
                                                        }

                                                        drawPath(
                                                            path = path,
                                                            color = lineColor,
                                                            style = Stroke(
                                                                width = strokeW,
                                                                cap = StrokeCap.Round,
                                                                join = StrokeJoin.Round
                                                            )
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.width(6.dp))

                                                    // Episode card with indentation
                                                    Row(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .bounceClick(scaleDown = 0.98f) {
                                                                activeEdit = TargetEdit(
                                                                    seasonNumber = seasonNum,
                                                                    episodeNumber = ep,
                                                                    currentLocalDate = parsedDate
                                                                )
                                                            }
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .background(Color.White.copy(alpha = 0.04f))
                                                            .border(0.5.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(12.dp))
                                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = "S${seasonNum.toString().padStart(2, '0')} E${ep.toString().padStart(2, '0')}" +
                                                                        if (!episodeTitle.isNullOrBlank()) " • $episodeTitle" else "",
                                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                                                color = Color.White,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            Text(
                                                                text = displayDate,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = Color.White.copy(alpha = 0.6f)
                                                            )
                                                        }

                                                        Box(
                                                            modifier = Modifier
                                                                .size(32.dp)
                                                                .clip(CircleShape)
                                                                .background(accentColor.copy(alpha = 0.15f)),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_pencil),
                                                                contentDescription = "Edit date",
                                                                tint = accentColor,
                                                                modifier = Modifier.size(15.dp)
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
                }
            }
        }
    }
}

    // DatePicker for the active target
    activeEdit?.let { target ->
        FlickTroveDatePickerModal(
            initialDate = target.currentLocalDate,
            isVisible = true,
            onDateSelected = { selectedDate ->
                val newIso = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toString()
                if (target.episodeNumber != null) {
                    onUpdateEpisodeDate(target.seasonNumber, target.episodeNumber, newIso)
                } else {
                    val allSeasonEps = sessionEpisodes[target.seasonNumber.toString()] ?: emptyList()
                    onUpdateSeasonDate(target.seasonNumber, allSeasonEps, newIso)
                }
                activeEdit = null
            },
            onDismissRequest = { activeEdit = null },
            hazeState = hazeState,
            accentColor = accentColor
        )
    }
}
