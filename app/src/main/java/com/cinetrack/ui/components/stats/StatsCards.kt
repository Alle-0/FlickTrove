package com.cinetrack.ui.components.stats

import com.cinetrack.R
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.util.buildTmdbImageUrl
import com.cinetrack.util.ImageType
import com.cinetrack.util.ImageQuality
import com.cinetrack.util.LocalImageQuality
import com.cinetrack.ui.viewmodel.PersonStat
import com.cinetrack.ui.viewmodel.StudioStat
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowDown
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowUp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest
import com.cinetrack.util.WhiteLogoTransformation
import kotlin.math.roundToInt

// ════════════════════════════════════════════════════════════════════
// Total Time Hero
// ════════════════════════════════════════════════════════════════════

@Composable
fun TotalTimeHeroCard(totalMinutes: Int) {
    var currentMinutes by rememberSaveable { mutableStateOf(0) }
    LaunchedEffect(totalMinutes) {
        currentMinutes = totalMinutes
    }
    
    val animatedMinutes by animateIntAsState(
        targetValue = currentMinutes,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "totalMinutes"
    )

    // Format minutes on the fly for the animation effect
    val d = animatedMinutes / 1440
    val h = (animatedMinutes % 1440) / 60
    val m = animatedMinutes % 60
    val timeFormatted = when {
        d > 0 -> stringResource(R.string.stats_dhm, d, h, m)
        h > 0 -> stringResource(R.string.stats_hm, h, m)
        else -> stringResource(R.string.stats_m, m)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statsCard(RoundedCornerShape(24.dp))
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(ImageVector.vectorResource(id = R.drawable.ic_clock), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.stats_total_time),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp
                    )
                }
                
                val context = androidx.compose.ui.platform.LocalContext.current
                val days = totalMinutes / 1440
                val hours = (totalMinutes % 1440) / 60
                val minutes = totalMinutes % 60
                val time = when {
                    days > 0 -> stringResource(R.string.stats_dhm, days, hours, minutes)
                    hours > 0 -> stringResource(R.string.stats_hm, hours, minutes)
                    else -> stringResource(R.string.stats_m, minutes)
                }
                val shareText = stringResource(R.string.stats_share_text, time)

                IconButton(
                    onClick = {
                        val sendIntent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        context.startActivity(android.content.Intent.createChooser(sendIntent, null))
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(ImageVector.vectorResource(id = R.drawable.ic_share), null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(14.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
                Text(
                    timeFormatted,
                    color = Color.White,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.5).sp
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.stats_total_time_combined),
                color = Color.White.copy(alpha = 0.25f),
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// Section Header
// ════════════════════════════════════════════════════════════════════

@Composable
fun StatsSectionHeader(
    icon: ImageVector,
    title: String,
    count: Int?,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
    ) {
        // Icon inside a rounded pill box
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(
            title,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.5.sp,
            modifier = Modifier.weight(1f)
        )
        
        if (actionLabel != null && onActionClick != null) {
            Row(
                modifier = Modifier
                    .bounceClick { onActionClick() }
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = if (actionLabel.contains("MENO", ignoreCase = true)) ImageVector.vectorResource(id = R.drawable.ic_left) else ImageVector.vectorResource(id = R.drawable.ic_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
        } else if (count != null) {
            CountingText(target = count, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Black)
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// Mini stat card
// ════════════════════════════════════════════════════════════════════

@Composable
fun MiniStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .statsCard(RoundedCornerShape(16.dp))
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        value,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(label, color = Color.White.copy(alpha = 0.3f), fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// Animated counting number
// ════════════════════════════════════════════════════════════════════

@Composable
fun CountingText(
    target: Int,
    color: Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight,
    letterSpacing: androidx.compose.ui.unit.TextUnit = 0.sp,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    suffixFontSize: androidx.compose.ui.unit.TextUnit = fontSize,
    textAlign: TextAlign? = null
) {
    var currentValue by rememberSaveable { mutableStateOf(0) }
    LaunchedEffect(target) {
        currentValue = target
    }

    val animatedValue by animateIntAsState(
        targetValue = currentValue,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "count"
    )
    if (suffix != null) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = color, fontSize = fontSize, fontWeight = fontWeight, letterSpacing = letterSpacing)) {
                    append(animatedValue.toString())
                }
                withStyle(SpanStyle(color = color.copy(alpha = 0.5f), fontSize = suffixFontSize, fontWeight = FontWeight.Medium)) {
                    append(" $suffix")
                }
            },
            modifier = modifier.wrapContentSize(unbounded = true),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Visible,
            textAlign = textAlign,
            lineHeight = fontSize
        )
    } else {
        Text(
            text = animatedValue.toString(),
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            letterSpacing = letterSpacing,
            modifier = modifier,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Visible,
            textAlign = textAlign,
            lineHeight = fontSize
        )
    }
}

// ════════════════════════════════════════════════════════════════════
// Dual stat pill  (single surface, divider, two halves)
// ════════════════════════════════════════════════════════════════════

@Composable
fun DualStatPill(
    leftLabel: String,
    leftValue: Int,
    leftIcon: ImageVector,
    rightLabel: String,
    rightValue: Int,
    rightIcon: ImageVector,
    accentColor: Color,
    rightSuffix: String? = null
) {
    Box(
        modifier = Modifier.fillMaxWidth()
            .statsCard(RoundedCornerShape(20.dp))
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left half
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(leftIcon, null, tint = accentColor, modifier = Modifier.size(24.dp))
                }
                Column {
                    CountingText(
                        target = leftValue,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    )
                    Text(leftLabel, color = Color.White.copy(alpha = 0.45f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
            }
            // Central vertical divider
            Box(
                Modifier
                    .width(1.dp)
                    .height(34.dp)
                    .background(Color.White.copy(alpha = 0.08f))
            )
            // Right half
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(rightIcon, null, tint = accentColor, modifier = Modifier.size(24.dp))
                }
                Column {
                    CountingText(
                        target = rightValue,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                        suffix = rightSuffix,
                        suffixFontSize = 11.sp
                    )
                    Text(rightLabel, color = Color.White.copy(alpha = 0.3f), fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// Combined media time + longest card (like the reference image)
// ════════════════════════════════════════════════════════════════════

@Composable
fun MediaTimeCard(
    timeLabel: String,
    time: String,
    longestLabel: String,
    longestTitle: String?,
    longestDurationMinutes: Int,
    accentColor: Color,
    sectionIcon: ImageVector,
    longestSuffix: String? = null,
    longestPosterPath: String? = null,
    onLongestItemClick: (() -> Unit)? = null
) {
    val d = longestDurationMinutes / 1440
    val h = (longestDurationMinutes % 1440) / 60
    val m = longestDurationMinutes % 60
    val durText = buildString {
        when {
            d > 0 -> append(stringResource(R.string.stats_dhm, d, h, m))
            h > 0 -> append(stringResource(R.string.stats_hm, h, m))
            else -> append(stringResource(R.string.stats_m, m))
        }
        if (longestSuffix != null) append(" $longestSuffix")
    }

    Box(
        modifier = Modifier.fillMaxWidth()
            .statsCard(RoundedCornerShape(28.dp))
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {

            // === Top row: total time ===
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(ImageVector.vectorResource(id = R.drawable.ic_clock), null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(timeLabel, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(time, color = accentColor, fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.4).sp)
                }
            }

            // === Longest row (below the time) ===
            if (!longestTitle.isNullOrBlank() && longestDurationMinutes > 0) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .then(if (onLongestItemClick != null) Modifier.bounceClick { onLongestItemClick() } else Modifier)
                        .padding(vertical = 4.dp)
                ) {
                    if (longestPosterPath != null) {
                        Box(
                            modifier = Modifier
                                .height(56.dp)
                                .width(38.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                        ) {
                            coil.compose.AsyncImage(
                                model = buildTmdbImageUrl(longestPosterPath, ImageType.POSTER, LocalImageQuality.current),
                                contentDescription = null,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                    }
                    
                    Column(Modifier.weight(1f)) {
                        Text(
                            longestLabel,
                            color = accentColor.copy(alpha = 0.55f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            longestTitle,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(durText, color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}


// ════════════════════════════════════════════════════════════════════
// Person horizontal list (cast / directors)
// ════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersonSection(
    people: List<PersonStat>,
    accentColor: Color,
    isExpanded: Boolean,
    onPersonClick: (Long, String?) -> Unit = { _, _ -> }
) {
    if (isExpanded) {
        // Full grid when expanded
        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(18.dp),
            maxItemsInEachRow = 4
        ) {
            people.forEach { person ->
                PersonAvatar(person = person, accentColor = accentColor, onPersonClick = onPersonClick)
            }
        }
    } else {
        // Horizontal list when collapsed
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            items(people.take(10), key = { it.id }, contentType = { "person" }) { person ->
                PersonAvatar(person = person, accentColor = accentColor, onPersonClick = onPersonClick)
            }
        }
    }
}

@Composable
fun PersonAvatar(
    person: PersonStat,
    accentColor: Color,
    onPersonClick: (Long, String?) -> Unit = { _, _ -> }
) {
    val avatarSize = 66.dp
    val badgeSize  = 22.dp
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .bounceClick { onPersonClick(person.id, person.profilePath) }
    ) {
        Box(modifier = Modifier.size(avatarSize)) {
            // Outer teal ring
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.5.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(50)),
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = person.name.split(" ")
                            .mapNotNull { it.firstOrNull()?.toString() }
                            .take(2)
                            .joinToString(""),
                        color = accentColor.copy(alpha = 0.7f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                if (!person.profilePath.isNullOrBlank()) {
                    AsyncImage(
                        model = buildTmdbImageUrl(person.profilePath, ImageType.PROFILE, LocalImageQuality.current),
                        contentDescription = person.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            // Badge: teal pill with count, bottom-right
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .defaultMinSize(minWidth = badgeSize)
                    .height(badgeSize)
                    .background(accentColor, RoundedCornerShape(50))
                    .border(2.dp, Color(0xFF0D1117), RoundedCornerShape(50))
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                CountingText(
                    target = person.count,
                    color = Color.Black,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = person.name,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 13.sp
        )
    }
}

// ════════════════════════════════════════════════════════════════════
// Studio Podium Chart (Top 3 Studios on Pedestals)
// ════════════════════════════════════════════════════════════════════

private data class StudioPodiumItem(
    val studio: StudioStat,
    val rank: Int,
    val pedestalHeight: androidx.compose.ui.unit.Dp,
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
        // 1. Rank Badge & Trophy at the TOP (Prominent & Clear, elevated above logo)
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

        // 2. Studio Logo / Name Box (Resting directly on top of the pedestal)
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(34.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF161922))
                .border(1.dp, item.color.copy(alpha = 0.40f), RoundedCornerShape(14.dp))
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

        // 3. Pedestal Box with Top Rim Neon Accent, Vertical Gradient & Percentages
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(animHeight)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            item.color.copy(alpha = 0.28f),
                            item.color.copy(alpha = 0.04f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(item.color.copy(alpha = 0.6f), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                )
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
    val displayStudios = if (isExpanded) studios else studios.take(5)
    val maxCount = studios.maxOfOrNull { it.count }?.toFloat() ?: 1f
    val totalCount = remember(studios) { studios.sumOf { it.count }.toFloat().coerceAtLeast(1f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statsCard(RoundedCornerShape(20.dp))
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

            Spacer(modifier = Modifier.height(4.dp))

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
                    // Studio Identifier: either the official TMDB logo in a sleek dark badge, or the studio name text
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
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF161922))
                                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
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

                    // Thick 12dp Capsule Progress Bar with horizontal gradient (identical to Top Countries)
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

                    // Clean Count + Percentage on the same line (identical to Genres/Countries)
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

            // Expand / Collapse Pill Button (identical to Genre & Country sections)
            if (studios.size > 5) {
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
