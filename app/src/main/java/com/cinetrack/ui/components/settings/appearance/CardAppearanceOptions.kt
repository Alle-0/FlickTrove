package com.cinetrack.ui.components.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.ui.components.card.EpisodeInfoStyle
import com.cinetrack.ui.components.common.FlickTroveSwitch
import com.cinetrack.ui.components.common.SegmentedControl
import com.cinetrack.ui.components.common.SegmentedOption
import com.cinetrack.ui.utils.bounceClick

@Composable
fun AppearanceSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                fontSize = 11.5.sp
            ),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
            modifier = Modifier.padding(start = 2.dp)
        )
        content()
    }
}

// ─── Compact Episode Style Options ──────────────────────────────────────────

@Composable
fun EpisodeStyleCompactOptions(
    selected: String,
    accentColor: Color,
    vibrationEnabled: Boolean,
    context: android.content.Context,
    onSelect: (String) -> Unit
) {
    val normRemaining = if (selected == EpisodeInfoStyle.REMAINING || selected == "REMAINING_AND_TOTAL") {
        EpisodeInfoStyle.REMAINING_SEASON_AND_TOTAL
    } else selected

    val normPercentage = if (selected == EpisodeInfoStyle.PERCENTAGE) {
        EpisodeInfoStyle.PERCENTAGE_TOTAL
    } else selected

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // ── 1. Rimanenti (Left) ──
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                text = stringResource(R.string.settings_cat_remaining).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = Color.White.copy(alpha = 0.45f)
            )
            SegmentedControl(
                options = listOf(
                    SegmentedOption(
                        value = EpisodeInfoStyle.REMAINING_SEASON,
                        title = stringResource(R.string.settings_chip_col_season),
                        subtitle = stringResource(R.string.settings_sub_rem_season)
                    ),
                    SegmentedOption(
                        value = EpisodeInfoStyle.REMAINING_TOTAL,
                        title = stringResource(R.string.settings_chip_col_series),
                        subtitle = stringResource(R.string.settings_sub_rem_series)
                    ),
                    SegmentedOption(
                        value = EpisodeInfoStyle.REMAINING_SEASON_AND_TOTAL,
                        title = stringResource(R.string.settings_chip_col_both),
                        subtitle = stringResource(R.string.settings_sub_rem_both)
                    )
                ),
                selected = normRemaining,
                accentColor = accentColor,
                vibrationEnabled = vibrationEnabled,
                context = context,
                onSelect = onSelect
            )
        }

        // ── 2. Visti / Totale ( / ) ──
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                text = stringResource(R.string.settings_cat_watched).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = Color.White.copy(alpha = 0.45f)
            )
            SegmentedControl(
                options = listOf(
                    SegmentedOption(
                        value = EpisodeInfoStyle.SEASON_ONLY,
                        title = stringResource(R.string.settings_chip_col_season),
                        subtitle = stringResource(R.string.settings_sub_watched_season)
                    ),
                    SegmentedOption(
                        value = EpisodeInfoStyle.WATCHED_OVER_TOTAL,
                        title = stringResource(R.string.settings_chip_col_series),
                        subtitle = stringResource(R.string.settings_sub_watched_series)
                    ),
                    SegmentedOption(
                        value = EpisodeInfoStyle.SEASON_AND_TOTAL,
                        title = stringResource(R.string.settings_chip_col_both),
                        subtitle = stringResource(R.string.settings_sub_watched_both)
                    )
                ),
                selected = selected,
                accentColor = accentColor,
                vibrationEnabled = vibrationEnabled,
                context = context,
                onSelect = onSelect
            )
        }

        // ── 3. Percentuale (%) ──
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                text = stringResource(R.string.settings_cat_percentage).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = Color.White.copy(alpha = 0.45f)
            )
            SegmentedControl(
                options = listOf(
                    SegmentedOption(
                        value = EpisodeInfoStyle.PERCENTAGE_SEASON,
                        title = stringResource(R.string.settings_chip_col_season),
                        subtitle = stringResource(R.string.settings_sub_pct_season)
                    ),
                    SegmentedOption(
                        value = EpisodeInfoStyle.PERCENTAGE_TOTAL,
                        title = stringResource(R.string.settings_chip_col_series),
                        subtitle = stringResource(R.string.settings_sub_pct_series)
                    ),
                    SegmentedOption(
                        value = EpisodeInfoStyle.PERCENTAGE_SEASON_AND_TOTAL,
                        title = stringResource(R.string.settings_chip_col_both),
                        subtitle = stringResource(R.string.settings_sub_pct_both)
                    )
                ),
                selected = normPercentage,
                accentColor = accentColor,
                vibrationEnabled = vibrationEnabled,
                context = context,
                onSelect = onSelect
            )
        }
    }
}

// ─── Compact Progress Bar Options ───────────────────────────────────────────

@Composable
fun ProgressBarCompactOptions(
    selectedLinear: String,
    selectedCircular: String,
    accentColor: Color,
    vibrationEnabled: Boolean,
    context: android.content.Context,
    isLinearEnabled: Boolean = true,
    onSelectLinear: (String) -> Unit,
    onSelectCircular: (String) -> Unit
) {
    val linearLabelAlpha by animateFloatAsState(
        targetValue = if (isLinearEnabled) 0.45f else 0.20f,
        animationSpec = tween(200),
        label = "linearLabelAlpha"
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // ── 1. Barra Lineare ──
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                text = stringResource(R.string.settings_cat_linear_bar).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = Color.White.copy(alpha = linearLabelAlpha)
            )
            SegmentedControl(
                options = listOf(
                    SegmentedOption(
                        value = "NONE",
                        title = stringResource(R.string.settings_chip_disabled),
                        subtitle = stringResource(R.string.settings_chip_progress_none)
                    ),
                    SegmentedOption(
                        value = "TOTAL",
                        title = stringResource(R.string.settings_chip_col_series),
                        subtitle = stringResource(R.string.settings_chip_whole_series)
                    ),
                    SegmentedOption(
                        value = "SEASON",
                        title = stringResource(R.string.settings_chip_col_season),
                        subtitle = stringResource(R.string.settings_chip_current_season)
                    )
                ),
                selected = selectedLinear,
                accentColor = accentColor,
                vibrationEnabled = vibrationEnabled,
                context = context,
                enabled = isLinearEnabled,
                onSelect = onSelectLinear
            )
        }

        // ── 2. Barra Circolare ──
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                text = stringResource(R.string.settings_cat_circular_bar).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = Color.White.copy(alpha = 0.45f)
            )
            SegmentedControl(
                options = listOf(
                    SegmentedOption(
                        value = "TOTAL",
                        title = stringResource(R.string.settings_chip_col_series),
                        subtitle = stringResource(R.string.settings_chip_whole_series)
                    ),
                    SegmentedOption(
                        value = "SEASON",
                        title = stringResource(R.string.settings_chip_col_season),
                        subtitle = stringResource(R.string.settings_chip_current_season)
                    )
                ),
                selected = selectedCircular,
                accentColor = accentColor,
                vibrationEnabled = vibrationEnabled,
                context = context,
                onSelect = onSelectCircular
            )
        }
    }
}

// ─── Text Size Options ────────────────────────────────────────────────────

@Composable
fun TextSizeOptions(
    selected: Float,
    accentColor: Color,
    vibrationEnabled: Boolean,
    context: android.content.Context,
    onSelect: (Float) -> Unit
) {
    SegmentedControl(
        options = listOf(
            SegmentedOption(value = 0.8f, title = stringResource(R.string.settings_size_small)),
            SegmentedOption(value = 1.0f, title = stringResource(R.string.settings_size_medium)),
            SegmentedOption(value = 1.2f, title = stringResource(R.string.settings_size_large))
        ),
        selected = selected,
        accentColor = accentColor,
        vibrationEnabled = vibrationEnabled,
        context = context,
        height = 36.dp,
        onSelect = onSelect
    )
}

// ─── Folder Bookmarks Toggle Option ─────────────────────────────────────────

@Composable
fun FolderBookmarksToggleOption(
    showFolderBookmarks: Boolean,
    accentColor: Color,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(scaleDown = 0.98f, onLongClick = {}) { onToggle(!showFolderBookmarks) }
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_folder_bookmarks),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White.copy(alpha = 0.9f)
            )
            Text(
                text = stringResource(R.string.settings_folder_bookmarks_desc),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.45f)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        FlickTroveSwitch(
            checked = showFolderBookmarks,
            onCheckedChange = onToggle,
            accentColor = accentColor
        )
    }
}

// ─── Badge Toggle Option ────────────────────────────────────────────────────

@Composable
fun BadgeToggleOption(
    showBadges: Boolean,
    accentColor: Color,
    onShowBadgesInfo: () -> Unit = {},
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(scaleDown = 0.98f, onLongClick = {}) { onToggle(!showBadges) }
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_badges),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White.copy(alpha = 0.9f)
            )
            Text(
                text = stringResource(R.string.settings_badges_desc),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.45f)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .bounceClick { onShowBadgesInfo() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = "Info Badge",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            FlickTroveSwitch(
                checked = showBadges,
                onCheckedChange = onToggle,
                accentColor = accentColor
            )
        }
    }
}
