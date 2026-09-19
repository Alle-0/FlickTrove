package com.cinetrack.ui.components.stats

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowDown
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cinetrack.R
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.viewmodel.StudioStat
import com.cinetrack.util.ImageType
import com.cinetrack.util.LocalImageQuality
import com.cinetrack.util.WhiteLogoTransformation
import com.cinetrack.util.buildTmdbImageUrl
import kotlin.math.roundToInt

// ════════════════════════════════════════════════════════════════════
// Studio Podium Chart (Top 3 Studios on Pedestals)
// ════════════════════════════════════════════════════════════════════

private data class StudioPodiumItem(
    val studio: StudioStat,
    val rank: Int,
    val pedestalHeight: Dp,
    val color: Color,
    val glowColor: Color,
    val percentageText: String
)

@Composable
private fun StudioPodiumChart(
    topStudios: List<StudioStat>,
    totalCount: Float,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    if (topStudios.isEmpty()) return

    fun calcPct(count: Int): String {
        val pct = ((count.toFloat() / totalCount) * 100).roundToInt()
        return if (pct == 0 && count > 0) "<1%" else "$pct%"
    }

    val podiumItems = remember(topStudios, totalCount) {
        when {
            topStudios.size >= 3 -> listOf(
                StudioPodiumItem(
                    studio = topStudios[1],
                    rank = 2,
                    pedestalHeight = 84.dp,
                    color = Color(0xFFCBD5E1),
                    glowColor = Color(0xFF94A3B8),
                    percentageText = calcPct(topStudios[1].count)
                ),
                StudioPodiumItem(
                    studio = topStudios[0],
                    rank = 1,
                    pedestalHeight = 112.dp,
                    color = Color(0xFFFFB800),
                    glowColor = accentColor,
                    percentageText = calcPct(topStudios[0].count)
                ),
                StudioPodiumItem(
                    studio = topStudios[2],
                    rank = 3,
                    pedestalHeight = 64.dp,
                    color = Color(0xFFCD7F32),
                    glowColor = Color(0xFFEA580C),
                    percentageText = calcPct(topStudios[2].count)
                )
            )
            topStudios.size == 2 -> listOf(
                StudioPodiumItem(
                    studio = topStudios[1],
                    rank = 2,
                    pedestalHeight = 84.dp,
                    color = Color(0xFFCBD5E1),
                    glowColor = Color(0xFF94A3B8),
                    percentageText = calcPct(topStudios[1].count)
                ),
                StudioPodiumItem(
                    studio = topStudios[0],
                    rank = 1,
                    pedestalHeight = 112.dp,
                    color = Color(0xFFFFB800),
                    glowColor = accentColor,
                    percentageText = calcPct(topStudios[0].count)
                )
            )
            else -> listOf(
                StudioPodiumItem(
                    studio = topStudios[0],
                    rank = 1,
                    pedestalHeight = 112.dp,
                    color = Color(0xFFFFB800),
                    glowColor = accentColor,
                    percentageText = calcPct(topStudios[0].count)
                )
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(215.dp)
            .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(20.dp))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            podiumItems.forEachIndexed { idx, item ->
                PodiumColumn(
                    item = item,
                    delay = idx * 100,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PodiumColumn(
    item: StudioPodiumItem,
    delay: Int,
    modifier: Modifier = Modifier
) {
    val animHeight by animateDpAsState(
        targetValue = item.pedestalHeight,
        animationSpec = tween(900, delayMillis = delay, easing = FastOutSlowInEasing),
        label = "podium_height_${item.rank}"
    )

    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        // 1. Rank Badge & Trophy at the TOP
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(bottom = 10.dp)
        ) {
            if (item.rank == 1) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_trophy),
                    contentDescription = null,
                    tint = Color(0xFFFFB800),
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(4.dp))
            }
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(item.color.copy(alpha = 0.22f), CircleShape)
                    .border(1.5.dp, item.color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.rank.toString(),
                    color = item.color,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // 2. Studio Logo / Name Box
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(34.dp)
                .background(Color(0xFF161922), RoundedCornerShape(14.dp))
                .border(1.dp, item.color.copy(alpha = 0.40f), RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (!item.studio.logoPath.isNullOrBlank()) {
                val context = LocalContext.current
                val logoUrl = buildTmdbImageUrl(item.studio.logoPath, ImageType.LOGO, LocalImageQuality.current)
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(logoUrl)
                        .crossfade(true)
                        .transformations(WhiteLogoTransformation())
                        .build(),
                    contentDescription = item.studio.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = item.studio.name,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // 3. Pedestal Box
        val pedestalShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(animHeight)
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            item.color.copy(alpha = 0.28f),
                            item.color.copy(alpha = 0.04f)
                        )
                    ),
                    shape = pedestalShape
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(item.color.copy(alpha = 0.6f), Color.Transparent)
                    ),
                    shape = pedestalShape
                )
                .clip(pedestalShape)
                .padding(top = 6.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = item.studio.count.toString(),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp
                        )
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = item.percentageText,
                        color = item.color,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = stringResource(R.string.stats_watched).uppercase(),
                    color = Color.White.copy(alpha = 0.35f),
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// Studio & Production Distribution Section
// ════════════════════════════════════════════════════════════════════

@Composable
fun StudioDistributionSection(
    studios: List<StudioStat>,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    if (studios.isEmpty()) return

    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val remainingStudios = remember(studios) { studios.drop(3) }
    val displayStudios = if (isExpanded) remainingStudios else remainingStudios.take(5)
    val maxCount = studios.maxOfOrNull { it.count }?.toFloat() ?: 1f
    val totalCount = remember(studios) { studios.sumOf { it.count }.toFloat().coerceAtLeast(1f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statsCard(RoundedCornerShape(32.dp))
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Visual Studio Podium Chart at the top
            StudioPodiumChart(
                topStudios = studios.take(3),
                totalCount = totalCount,
                accentColor = accentColor
            )

            if (remainingStudios.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))

                val scrollState = rememberScrollState()
                val nestedScrollConnection = remember {
                    object : NestedScrollConnection {
                        override fun onPostScroll(
                            consumed: Offset,
                            available: Offset,
                            source: NestedScrollSource
                        ): Offset {
                            return available
                        }
                    }
                }

                Box(
                    modifier = if (isExpanded) {
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp)
                            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                            .drawWithContent {
                                drawContent()
                                if (scrollState.value > 0) {
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            0f to Color.Transparent,
                                            1f to Color.Black,
                                            startY = 0f,
                                            endY = 28.dp.toPx()
                                        ),
                                        blendMode = BlendMode.DstIn
                                    )
                                }
                                if (scrollState.value < scrollState.maxValue) {
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            0f to Color.Black,
                                            1f to Color.Transparent,
                                            startY = size.height - 28.dp.toPx(),
                                            endY = size.height
                                        ),
                                        blendMode = BlendMode.DstIn
                                    )
                                }
                            }
                            .nestedScroll(nestedScrollConnection)
                            .verticalScroll(scrollState)
                    } else {
                        Modifier.fillMaxWidth()
                    }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        displayStudios.forEachIndexed { index, studio ->
                            val fraction = (studio.count.toFloat() / maxCount).coerceIn(0.04f, 1f)
                            val animFraction by animateFloatAsState(
                                targetValue = fraction,
                                animationSpec = tween(
                                    durationMillis = 900,
                                    delayMillis = (index * 80).coerceAtMost(400),
                                    easing = FastOutSlowInEasing
                                ),
                                label = "studio_bar_$index"
                            )

                            val pct = ((studio.count.toFloat() / totalCount) * 100).roundToInt()
                            val percentageText = if (pct == 0 && studio.count > 0) "<1%" else "$pct%"

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.width(96.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (!studio.logoPath.isNullOrBlank()) {
                                        val context = LocalContext.current
                                        val logoUrl = buildTmdbImageUrl(studio.logoPath, ImageType.LOGO, LocalImageQuality.current)
                                        Box(
                                            modifier = Modifier
                                                .width(90.dp)
                                                .height(30.dp)
                                                .background(Color(0xFF161922), RoundedCornerShape(50))
                                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(50))
                                                .clip(RoundedCornerShape(50))
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data(logoUrl)
                                                    .crossfade(true)
                                                    .transformations(WhiteLogoTransformation())
                                                    .build(),
                                                contentDescription = studio.name,
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = studio.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(12.dp)
                                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(6.dp)),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(animFraction)
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(accentColor.copy(alpha = 0.5f), accentColor)
                                                ),
                                                RoundedCornerShape(6.dp)
                                            )
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End,
                                    modifier = Modifier.widthIn(min = 48.dp)
                                ) {
                                    Text(
                                        text = studio.count.toString(),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp
                                        ),
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = percentageText,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 12.sp
                                        ),
                                        color = Color.White.copy(alpha = 0.35f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (remainingStudios.size > 5) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .bounceClick(onClick = { isExpanded = !isExpanded })
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isExpanded) stringResource(R.string.stats_see_less) else stringResource(R.string.stats_see_all),
                                style = MaterialTheme.typography.labelLarge,
                                color = accentColor,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Icon(
                                imageVector = if (isExpanded) Icons.Rounded.KeyboardDoubleArrowUp else Icons.Rounded.KeyboardDoubleArrowDown,
                                contentDescription = null,
                                tint = accentColor,
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
