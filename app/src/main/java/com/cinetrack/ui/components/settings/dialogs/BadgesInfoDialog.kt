package com.cinetrack.ui.components.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cinetrack.R
import com.cinetrack.ui.components.glass.GlassmorphicModal
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.theme.NeonPink
import com.cinetrack.ui.theme.NeonTeal
import com.cinetrack.ui.utils.premiumScrollbar
import com.cinetrack.ui.utils.verticalFadingEdges
import dev.chrisbanes.haze.HazeState

private data class BadgeInfoItem(
    val titleRes: Int,
    val color: Color,
    val descRes: Int
)

private val BADGES_LEGEND_LIST = listOf(
    BadgeInfoItem(R.string.settings_badge_new, NeonPink, R.string.settings_badge_new_desc),
    BadgeInfoItem(R.string.settings_badge_masterpiece, Color(0xFFFFD700), R.string.settings_badge_masterpiece_desc),
    BadgeInfoItem(R.string.settings_badge_best, Color(0xFF00E5FF), R.string.settings_badge_best_desc),
    BadgeInfoItem(R.string.settings_badge_hot, HazeStyles.AccentYellow, R.string.settings_badge_hot_desc),
    BadgeInfoItem(R.string.settings_badge_wow, NeonTeal, R.string.settings_badge_wow_desc),
    BadgeInfoItem(R.string.settings_badge_hidden_gem, Color(0xFF00E676), R.string.settings_badge_hidden_gem_desc),
    BadgeInfoItem(R.string.settings_badge_cult, Color(0xFF9C27B0), R.string.settings_badge_cult_desc),
    BadgeInfoItem(R.string.settings_badge_classic, Color(0xFF8D6E63), R.string.settings_badge_classic_desc),
    BadgeInfoItem(R.string.settings_badge_epic, Color(0xFFFF5722), R.string.settings_badge_epic_desc),
    BadgeInfoItem(R.string.settings_badge_binge, Color(0xFF00BCD4), R.string.settings_badge_binge_desc),
    BadgeInfoItem(R.string.settings_badge_scifi, Color(0xFF2962FF), R.string.settings_badge_scifi_desc),
    BadgeInfoItem(R.string.settings_badge_comedy, Color(0xFFFFEA00), R.string.settings_badge_comedy_desc),
    BadgeInfoItem(R.string.settings_badge_horror, Color(0xFFE53935), R.string.settings_badge_horror_desc),
    BadgeInfoItem(R.string.settings_badge_animation, Color(0xFFFF9800), R.string.settings_badge_animation_desc),
    BadgeInfoItem(R.string.settings_badge_blockbuster, Color(0xFF6200EA), R.string.settings_badge_blockbuster_desc),
    BadgeInfoItem(R.string.settings_badge_indie, Color(0xFFAED581), R.string.settings_badge_indie_desc),
    BadgeInfoItem(R.string.settings_badge_quick, Color(0xFFC6FF00), R.string.settings_badge_quick_desc),
    BadgeInfoItem(R.string.settings_badge_snack, Color(0xFFC6FF00), R.string.settings_badge_snack_desc),
    BadgeInfoItem(R.string.settings_badge_divisive, Color(0xFFFF9800), R.string.settings_badge_divisive_desc),
    BadgeInfoItem(R.string.settings_badge_vintage, Color(0xFFBCAAA4), R.string.settings_badge_vintage_desc),
    BadgeInfoItem(R.string.settings_badge_docu, Color(0xFF9E9E9E), R.string.settings_badge_docu_desc),
    BadgeInfoItem(R.string.settings_badge_family, Color(0xFF81D4FA), R.string.settings_badge_family_desc)
)

@Composable
fun BadgesInfoDialog(
    visible: Boolean,
    activeHazeState: HazeState,
    disabledBadges: Set<String>,
    onToggleBadge: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    GlassmorphicModal(
        visible = visible,
        activeHazeState = activeHazeState,
        zIndex = 150f,
        scrimAlpha = 0.50f,
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Rounded.Info,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    stringResource(R.string.settings_badges_meaning),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            val lazyListState = rememberLazyListState()

            LazyColumn(
                state = lazyListState,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .premiumScrollbar(lazyListState)
                    .padding(end = 12.dp)
                    .verticalFadingEdges(lazyListState, 16.dp, 16.dp)
            ) {
                items(
                    items = BADGES_LEGEND_LIST,
                    key = { it.titleRes }
                ) { item ->
                    val text = stringResource(item.titleRes)
                    BadgeLegendItem(
                        text = text,
                        color = item.color,
                        desc = stringResource(item.descRes),
                        enabled = !disabledBadges.contains(text),
                        onToggle = { enabled -> onToggleBadge(text, enabled) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            SettingsDialogConfirmButton(
                text = stringResource(R.string.settings_got_it),
                onClick = onDismiss,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.Black,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
