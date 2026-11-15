package com.cinetrack.ui.components.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.data.model.MoodHistoryEntry
import com.cinetrack.ui.components.common.CategoryTabSelector
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.mirrorInRtl
import com.cinetrack.ui.utils.rtlSafeLetterSpacing
import dev.chrisbanes.haze.HazeState

private data class MoodInspirationRes(
    val titleRes: Int,
    val promptRes: Int
)

private val MOOD_INSPIRATION_ITEMS = listOf(
    MoodInspirationRes(R.string.mood_insp_1_title, R.string.mood_insp_1_prompt),
    MoodInspirationRes(R.string.mood_insp_2_title, R.string.mood_insp_2_prompt),
    MoodInspirationRes(R.string.mood_insp_3_title, R.string.mood_insp_3_prompt),
    MoodInspirationRes(R.string.mood_insp_4_title, R.string.mood_insp_4_prompt),
    MoodInspirationRes(R.string.mood_insp_5_title, R.string.mood_insp_5_prompt),
    MoodInspirationRes(R.string.mood_insp_6_title, R.string.mood_insp_6_prompt),
    MoodInspirationRes(R.string.mood_insp_7_title, R.string.mood_insp_7_prompt),
    MoodInspirationRes(R.string.mood_insp_8_title, R.string.mood_insp_8_prompt)
)

@Composable
fun MoodDiscoverySection(
    remainingQuota: Int,
    vibeTitle: String?,
    vibeDescription: String?,
    errorMessage: String?,
    isLoading: Boolean,
    onMoodSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    isLocalEligible: Boolean = false,
    isMoodAiMode: Boolean = true,
    onMoodModeChanged: (Boolean) -> Unit = {},
    moodHistory: List<MoodHistoryEntry> = emptyList(),
    onSelectMoodHistory: (MoodHistoryEntry) -> Unit = {},
    onDeleteMoodHistory: (String) -> Unit = {},
    onClearMoodHistory: () -> Unit = {},
    hazeState: HazeState? = null
) {
    val accentColor = MaterialTheme.colorScheme.primary

    var isRecentExpanded by rememberSaveable { mutableStateOf(true) }
    var isInspirationsExpanded by rememberSaveable { mutableStateOf(true) }

    val recentChevronRotation by animateFloatAsState(
        targetValue = if (isRecentExpanded) 90f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "mood_recent_chevron"
    )

    val inspirationsChevronRotation by animateFloatAsState(
        targetValue = if (isInspirationsExpanded) 90f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "mood_inspirations_chevron"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // 1. Unified Cinematic Section Header & Mode Switcher in Single Row (Option A)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.search_mood_badge_label).uppercase(),
                modifier = Modifier.weight(1f, fill = false),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.40f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = rtlSafeLetterSpacing(1.2.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.width(8.dp))

            if (isLocalEligible) {
                Box(
                    modifier = Modifier
                        .then(
                            if (hazeState != null) {
                                Modifier.hazeGlass(state = hazeState, shape = CircleShape)
                            } else {
                                Modifier
                                    .background(Color.White.copy(alpha = 0.08f), CircleShape)
                                    .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CategoryTabSelector(
                        options = listOf(
                            stringResource(R.string.search_mood_mode_ai_tab, remainingQuota),
                            stringResource(R.string.search_mood_mode_local_tab)
                        ),
                        selectedIndex = if (isMoodAiMode) 0 else 1,
                        onOptionClick = { index -> onMoodModeChanged(index == 0) },
                        tabWidth = 68.dp,
                        fontSize = 10.sp,
                        uppercase = true
                    )
                }
            } else {
                Text(
                    text = if (remainingQuota > 0) {
                        stringResource(R.string.search_mood_quota_format, remainingQuota).uppercase()
                    } else {
                        stringResource(R.string.search_mood_quota_exhausted_short).uppercase()
                    },
                    color = if (remainingQuota > 0) accentColor.copy(alpha = 0.85f) else Color(0xFFFF6B6B).copy(alpha = 0.70f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = rtlSafeLetterSpacing(1.sp)
                )
            }
        }

        // 2. Cronologia Mood (Ricerche recenti memorizzate in locale - Chiudibile)
        if (moodHistory.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .bounceClick { isRecentExpanded = !isRecentExpanded }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_clock),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = stringResource(R.string.search_recent).uppercase(),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = rtlSafeLetterSpacing(1.sp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_right),
                        contentDescription = if (isRecentExpanded) stringResource(R.string.search_collapse) else stringResource(R.string.search_expand),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.40f),
                        modifier = Modifier
                            .size(11.dp)
                            .mirrorInRtl()
                            .rotate(recentChevronRotation)
                    )
                }

                Text(
                    text = stringResource(R.string.search_clear_all).uppercase(),
                    color = accentColor.copy(alpha = 0.85f),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = rtlSafeLetterSpacing(0.5.sp),
                    modifier = Modifier
                        .bounceClick { onClearMoodHistory() }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            AnimatedVisibility(
                visible = isRecentExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            val fadeWidthPx = 16.dp.toPx()
                            val fadeFraction = if (size.width > 0f) (fadeWidthPx / size.width).coerceAtMost(0.49f) else 0f
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    0f to Color.Transparent,
                                    fadeFraction to Color.Black,
                                    1f - fadeFraction to Color.Black,
                                    1f to Color.Transparent
                                ),
                                blendMode = BlendMode.DstIn
                            )
                        }
                ) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(moodHistory, key = { "${it.query}_${it.isAi}" }) { item ->
                            Box(
                                modifier = Modifier
                                    .height(32.dp)
                                    .bounceClick(scaleDown = 0.94f) {
                                        onSelectMoodHistory(item)
                                    }
                                    .background(Color.White.copy(alpha = 0.06f), CircleShape)
                                    .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
                                    .padding(start = 12.dp, end = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = item.query,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .bounceClick(scaleDown = 0.80f) {
                                                onDeleteMoodHistory(item.query)
                                            }
                                            .clip(CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_x),
                                            contentDescription = stringResource(R.string.search_remove_recent),
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.40f),
                                            modifier = Modifier.size(11.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Intestazione Ispirazioni Rapide (Chiudibile)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .bounceClick { isInspirationsExpanded = !isInspirationsExpanded }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_sparkle),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = stringResource(R.string.search_mood_inspirations_header).trimEnd(':').uppercase(),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = rtlSafeLetterSpacing(1.sp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_right),
                contentDescription = if (isInspirationsExpanded) stringResource(R.string.search_collapse) else stringResource(R.string.search_expand),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                modifier = Modifier
                    .size(11.dp)
                    .mirrorInRtl()
                    .rotate(inspirationsChevronRotation)
            )
        }

        AnimatedVisibility(
            visible = isInspirationsExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        val fadeWidthPx = 16.dp.toPx()
                        val fadeFraction = if (size.width > 0f) (fadeWidthPx / size.width).coerceAtMost(0.49f) else 0f
                        drawRect(
                            brush = Brush.horizontalGradient(
                                0f to Color.Transparent,
                                fadeFraction to Color.Black,
                                1f - fadeFraction to Color.Black,
                                1f to Color.Transparent
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
            ) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(MOOD_INSPIRATION_ITEMS, key = { it.titleRes }) { item ->
                        val title = stringResource(item.titleRes)
                        Box(
                            modifier = Modifier
                                .bounceClick(scaleDown = 0.94f, enabled = !isLoading) {
                                    onMoodSelected(title)
                                }
                                .height(34.dp)
                                .background(Color.White.copy(alpha = 0.06f), CircleShape)
                                .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = if (remainingQuota > 0) 0.85f else 0.35f
                                )
                            )
                        }
                    }
                }
            }
        }

        // 3. Error Message (Discreet banner without raw JSON dumps)
        if (!errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Red.copy(alpha = 0.08f))
                    .border(1.dp, Color.Red.copy(alpha = 0.20f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = errorMessage,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = Color(0xFFFF8A8A)
                )
            }
        }


    }
}
