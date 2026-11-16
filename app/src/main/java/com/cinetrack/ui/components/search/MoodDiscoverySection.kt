package com.cinetrack.ui.components.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.data.model.MoodHistoryEntry
import dev.chrisbanes.haze.HazeState

@Composable
fun MoodDiscoverySection(
    isLoading: Boolean,
    onMoodSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    remainingQuota: Int = 0,
    vibeTitle: String? = null,
    vibeDescription: String? = null,
    errorMessage: String? = null,
    isLocalEligible: Boolean = false,
    isMoodAiMode: Boolean = true,
    onMoodModeChanged: (Boolean) -> Unit = {},
    moodHistory: List<MoodHistoryEntry> = emptyList(),
    onSelectMoodHistory: (MoodHistoryEntry) -> Unit = {},
    onDeleteMoodHistory: (String) -> Unit = {},
    onClearMoodHistory: () -> Unit = {},
    isRecentExpanded: Boolean = true,
    isInspirationsExpanded: Boolean = true,
    onToggleRecentExpanded: () -> Unit = {},
    onToggleInspirationsExpanded: () -> Unit = {},
    hazeState: HazeState? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 56.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icona centrale a sfera in vetro con sparkle Teal
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.04f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_sparkle),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.search_mood_badge_label),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.search_mood_intro_text),
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 16.5.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 280.dp)
        )

        if (!errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Red.copy(alpha = 0.08f))
                    .border(1.dp, Color.Red.copy(alpha = 0.20f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = errorMessage,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = Color(0xFFFF8A8A),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
