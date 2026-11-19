package com.cinetrack.ui.components.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
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
    query: String = "",
    cursorPosition: Int = -1,
    remainingQuota: Int = 0,
    vibeTitle: String? = null,
    vibeDescription: String? = null,
    errorMessage: String? = null,
    isLocalEligible: Boolean = false,
    isMoodAiMode: Boolean = false,
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
            .padding(top = 10.dp, bottom = 40.dp)
            .graphicsLayer {
                val p = enterAnim.value
                alpha = p
                translationY = (1f - p) * 12f
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 1. Badge raffinato a capsula con micro-jewel
        AnimatedContent(
            targetState = isMoodAiMode,
            transitionSpec = {
                (fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing)))
                    .togetherWith(fadeOut(animationSpec = tween(140, easing = FastOutSlowInEasing)))
            },
            label = "badge_switch",
            contentAlignment = Alignment.Center
        ) { isAi ->
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = stringResource(if (isAi) R.string.search_mood_badge_ai else R.string.search_mood_iris_badge),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 1.2.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Titolo autorevole e fiero
        Text(
            text = stringResource(R.string.search_mood_badge_label),
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp
            ),
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 3. Testo descrittivo arioso con ottima leggibilità
        AnimatedContent(
            targetState = isMoodAiMode,
            transitionSpec = {
                (fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing)))
                    .togetherWith(fadeOut(animationSpec = tween(140, easing = FastOutSlowInEasing)))
            },
            label = "desc_switch",
            contentAlignment = Alignment.Center
        ) { isAi ->
            Text(
                text = stringResource(if (isAi) R.string.search_mood_intro_text else R.string.search_mood_iris_intro_text),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = Color.White.copy(alpha = 0.65f),
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 320.dp)
            )
        }

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
