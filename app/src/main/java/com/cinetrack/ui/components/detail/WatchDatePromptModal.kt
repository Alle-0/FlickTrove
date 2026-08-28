package com.cinetrack.ui.components.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cinetrack.R
import com.cinetrack.ui.components.shared.FlickTroveModal
import com.cinetrack.ui.components.shared.FlickTroveDatePickerModal
import com.cinetrack.ui.utils.bounceClick
import dev.chrisbanes.haze.HazeState
import java.time.Instant

@Composable
fun WatchDatePromptModal(
    hazeState: HazeState,
    accentColor: Color,
    releaseDate: Instant?,
    onDismiss: () -> Unit,
    onDateSelected: (Instant?) -> Unit // null means 'now'
) {
    var showDatePicker by remember { mutableStateOf(false) }

    val modalVisibilityAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (showDatePicker) 0f else 1f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 300),
        label = "modalVisibilityAlpha"
    )

    FlickTroveDatePickerModal(
        initialDate = java.time.LocalDate.now(),
        isVisible = showDatePicker,
        onDateSelected = { localDate ->
            onDateSelected(localDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())
            showDatePicker = false
        },
        onDismissRequest = { showDatePicker = false },
        hazeState = hazeState,
        accentColor = accentColor
    )

    Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = modalVisibilityAlpha }) {
        FlickTroveModal(
            hazeState = hazeState,
            onDismissRequest = onDismiss
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.watch_date_prompt_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .bounceClick(scaleDown = 0.9f, onClick = onDismiss)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_x),
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Options
                WatchDateOption(
                    iconRes = R.drawable.ic_clock,
                    label = stringResource(id = R.string.watch_date_prompt_now),
                    accentColor = accentColor,
                    onClick = { onDateSelected(null) }
                )

                if (releaseDate != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    WatchDateOption(
                        iconRes = R.drawable.ic_calendario,
                        label = stringResource(id = R.string.watch_date_prompt_release_date),
                        accentColor = accentColor,
                        onClick = { onDateSelected(releaseDate) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                WatchDateOption(
                    iconRes = R.drawable.ic_pencil,
                    label = stringResource(id = R.string.watch_date_prompt_custom),
                    accentColor = accentColor,
                    onClick = { showDatePicker = true }
                )
            }
        }
    }
}

@Composable
private fun WatchDateOption(
    iconRes: Int,
    label: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .bounceClick(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = iconRes),
            contentDescription = label,
            tint = accentColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White
        )
    }
}
