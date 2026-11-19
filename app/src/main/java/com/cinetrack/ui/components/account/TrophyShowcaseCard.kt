package com.cinetrack.ui.components.account

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.cinetrack.R
import com.cinetrack.ui.components.badge.FlickTroveBadgeEmblem
import com.cinetrack.ui.components.badge.OFFICIAL_TROPHY_ROOM_CATALOG
import com.cinetrack.ui.components.badge.TrophyRoomItemUi
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.mirrorInRtl
import dev.chrisbanes.haze.HazeState

@Composable
fun TrophyShowcaseCard(
    hazeState: HazeState,
    backgroundLuminance: Float = 0f,
    isEnabled: Boolean = true,
    unlockedBadges: List<TrophyRoomItemUi> = emptyList(),
    onClick: () -> Unit
) {
    val cardOverlay = if (backgroundLuminance > 0.38f) Color.Black.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.05f)
    val cardAlpha = if (isEnabled) 1f else 0.45f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .hazeGlass(
                    state = hazeState,
                    shape = RoundedCornerShape(32.dp),
                    containerColor = cardOverlay,
                    borderColor = Color.White.copy(alpha = if (isEnabled) 0.12f else 0.05f),
                    borderWidth = 1.dp,
                    useOffscreenStrategy = false
                )
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 16.dp)
                .graphicsLayer { alpha = cardAlpha }
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isEnabled) {
                            Modifier.bounceClick { onClick() }
                        } else {
                            Modifier
                        }
                    )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_trophy),
                    contentDescription = stringResource(R.string.dashboard_trophies),
                    tint = if (isEnabled) Color.White else Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.dashboard_trophies).uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.weight(1f)
                )

                if (isEnabled) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_right),
                        contentDescription = "Open Trophy Room",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier
                            .size(20.dp)
                            .mirrorInRtl()
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_lock),
                            contentDescription = stringResource(R.string.dashboard_trophies_coming_soon),
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.dashboard_trophies_coming_soon),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.dashboard_trophies_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = if (isEnabled) 0.75f else 0.5f),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Overlapping preview emblems from the last 3 unlocked badges or official catalog
                val previewEmblems = remember(unlockedBadges) {
                    if (unlockedBadges.isNotEmpty()) {
                        unlockedBadges.sortedByDescending { it.unlockedDate ?: "" }.take(3)
                    } else {
                        OFFICIAL_TROPHY_ROOM_CATALOG.take(3)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy((-10).dp)
                ) {
                    previewEmblems.forEachIndexed { index, item ->
                        Box(
                            modifier = Modifier
                                .zIndex((index + 1).toFloat())
                                .size(34.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            FlickTroveBadgeEmblem(
                                tier = item.currentTier,
                                iconKind = item.badge.iconKind,
                                isUnlocked = isEnabled,
                                size = 34.dp
                            )
                        }
                    }
                }
            }
        }
    }
}
