package com.cinetrack.ui.components.stats

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cinetrack.R
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.util.ImageType
import com.cinetrack.util.LocalImageQuality
import com.cinetrack.util.buildTmdbImageUrl

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
            .statsCard(RoundedCornerShape(32.dp))
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
                
                val context = LocalContext.current
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
            .statsCard(RoundedCornerShape(24.dp))
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
// Combined media time + longest card
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
            .statsCard(RoundedCornerShape(32.dp))
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
                            AsyncImage(
                                model = buildTmdbImageUrl(longestPosterPath, ImageType.POSTER, LocalImageQuality.current),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
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
