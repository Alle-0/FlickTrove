package com.cinetrack.ui.components.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.ui.components.glass.GlassmorphicModal
import com.cinetrack.ui.theme.NeonTeal
import com.cinetrack.ui.utils.horizontalFadingEdges
import com.cinetrack.util.IconManager
import dev.chrisbanes.haze.HazeState

@Composable
fun SeasonalIconsPreviewDialog(
    visible: Boolean,
    activeHazeState: HazeState,
    seasonalAppIconEnabled: Boolean,
    onDismiss: () -> Unit
) {
    val isHalloweenActive = remember { com.cinetrack.util.IconManager.isHalloweenSeason() }
    val isChristmasActive = remember { com.cinetrack.util.IconManager.isChristmasSeason() }

    GlassmorphicModal(
        visible = visible,
        activeHazeState = activeHazeState,
        dimBackground = true,
        dismissOnClickOutside = true,
        onDismissRequest = onDismiss
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_seasonal_preview_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.settings_seasonal_preview_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalFadingEdges(scrollState, leftEdgeWidth = 24.dp, rightEdgeWidth = 24.dp)
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Classic Default Icon Card
                val isClassicActive = !seasonalAppIconEnabled || (!isHalloweenActive && !isChristmasActive)
                SeasonalIconCard(
                    title = stringResource(R.string.settings_seasonal_preview_classic),
                    subtitle = stringResource(R.string.settings_seasonal_preview_classic_desc),
                    drawableRes = R.drawable.ic_launcher_foreground_vector,
                    badgeText = if (isClassicActive) stringResource(R.string.settings_seasonal_preview_active_now) else null,
                    accentColor = NeonTeal,
                    modifier = Modifier.width(130.dp)
                )

                // Halloween Seasonal Icon Card
                val isHalloweenCurrentlyActive = seasonalAppIconEnabled && isHalloweenActive
                SeasonalIconCard(
                    title = stringResource(R.string.settings_seasonal_preview_halloween),
                    subtitle = stringResource(R.string.settings_seasonal_preview_halloween_desc),
                    drawableRes = R.drawable.ic_launcher_foreground_halloween,
                    badgeText = if (isHalloweenCurrentlyActive) {
                        stringResource(R.string.settings_seasonal_preview_active_now)
                    } else {
                        stringResource(R.string.settings_seasonal_preview_upcoming)
                    },
                    accentColor = Color(0xFFFF7518),
                    modifier = Modifier.width(130.dp)
                )

                // Christmas Seasonal Icon Card
                val isChristmasCurrentlyActive = seasonalAppIconEnabled && isChristmasActive
                SeasonalIconCard(
                    title = stringResource(R.string.settings_seasonal_preview_christmas),
                    subtitle = stringResource(R.string.settings_seasonal_preview_christmas_desc),
                    drawableRes = R.drawable.ic_launcher_foreground_christmas,
                    badgeText = if (isChristmasCurrentlyActive) {
                        stringResource(R.string.settings_seasonal_preview_active_now)
                    } else {
                        stringResource(R.string.settings_seasonal_preview_upcoming)
                    },
                    accentColor = Color(0xFFEF4444),
                    modifier = Modifier.width(130.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SettingsDialogCancelButton(
                text = stringResource(R.string.settings_confirm),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            )
        }
    }
}

@Composable
private fun SeasonalIconCard(
    title: String,
    subtitle: String,
    @androidx.annotation.DrawableRes drawableRes: Int,
    badgeText: String?,
    accentColor: Color = NeonTeal,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(vertical = 16.dp, horizontal = 10.dp)
    ) {
        // App icon frame (mimicking adaptive launcher icon)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF0F1216))
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(18.dp)
                )
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = drawableRes),
                contentDescription = null,
                modifier = Modifier.size(54.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = Color.White.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))
        if (badgeText != null) {
            Box(
                modifier = Modifier
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(accentColor.copy(alpha = 0.15f))
                    .border(
                        width = 1.dp,
                        color = accentColor.copy(alpha = 0.4f),
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = accentColor,
                    maxLines = 1
                )
            }
        } else {
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}
