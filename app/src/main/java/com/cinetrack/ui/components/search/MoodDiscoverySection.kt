package com.cinetrack.ui.components.search

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.data.model.MoodHistoryEntry
import com.cinetrack.ui.components.mascot.IrisMascot
import com.cinetrack.ui.components.mascot.IrisMood
import dev.chrisbanes.haze.HazeState

@Composable
fun MoodDiscoverySection(
    isLoading: Boolean,
    onMoodSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    query: String = "",
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
    val enterAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        enterAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "mood_discovery_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 56.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val effectiveMood = when {
            isLoading -> IrisMood.SEARCHING
            query.isNotBlank() -> IrisMood.TYPING
            else -> IrisMood.IDLE
        }

        // Mascotte Ottica IRIS con respiro vivace, battito periodico ed entrata elastica
        IrisMascot(
            mood = effectiveMood,
            size = 84.dp,
            modifier = Modifier.graphicsLayer {
                val p = enterAnim.value
                scaleX = (0.78f + 0.22f * p) * pulseScale
                scaleY = (0.78f + 0.22f * p) * pulseScale
                alpha = p
                translationY = (1f - p) * 24f
            }
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Badge ottico IRIS
        Box(
            modifier = Modifier
                .graphicsLayer {
                    val p = enterAnim.value
                    alpha = p
                    translationY = (1f - p) * 18f
                }
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = stringResource(R.string.search_mood_iris_badge),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.5.sp,
                    letterSpacing = 0.8.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.search_mood_badge_label),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer {
                val p = enterAnim.value
                alpha = p
                translationY = (1f - p) * 16f
            }
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.search_mood_intro_text),
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 16.5.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .graphicsLayer {
                    val p = enterAnim.value
                    alpha = p
                    translationY = (1f - p) * 12f
                }
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
