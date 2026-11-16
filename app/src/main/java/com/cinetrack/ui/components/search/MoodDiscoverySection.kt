package com.cinetrack.ui.components.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.data.model.MoodHistoryEntry
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.rtlSafeLetterSpacing
import dev.chrisbanes.haze.HazeState

private data class MoodInspirationRes(
    val titleRes: Int,
    val promptRes: Int,
    val tagText: String,
    val startColor: Color,
    val endColor: Color,
    val borderColor: Color,
    val tagColor: Color
)

private val MOOD_INSPIRATION_ITEMS = listOf(
    MoodInspirationRes(
        R.string.mood_insp_1_title,
        R.string.mood_insp_1_prompt,
        "01 / NOIR",
        Color(0xFF0F172A).copy(alpha = 0.75f),
        Color(0xFF1E293B).copy(alpha = 0.40f),
        Color(0xFF3B82F6).copy(alpha = 0.25f),
        Color(0xFF60A5FA)
    ),
    MoodInspirationRes(
        R.string.mood_insp_2_title,
        R.string.mood_insp_2_prompt,
        "02 / SCI-FI",
        Color(0xFF1E1B4B).copy(alpha = 0.75f),
        Color(0xFF2E1065).copy(alpha = 0.40f),
        Color(0xFF8B5CF6).copy(alpha = 0.25f),
        Color(0xFFA78BFA)
    ),
    MoodInspirationRes(
        R.string.mood_insp_3_title,
        R.string.mood_insp_3_prompt,
        "03 / COMEDY",
        Color(0xFF451A03).copy(alpha = 0.65f),
        Color(0xFF2E1505).copy(alpha = 0.40f),
        Color(0xFFF59E0B).copy(alpha = 0.25f),
        Color(0xFFFBBF24)
    ),
    MoodInspirationRes(
        R.string.mood_insp_4_title,
        R.string.mood_insp_4_prompt,
        "04 / HORROR",
        Color(0xFF064E3B).copy(alpha = 0.65f),
        Color(0xFF022C22).copy(alpha = 0.40f),
        Color(0xFF10B981).copy(alpha = 0.25f),
        Color(0xFF34D399)
    ),
    MoodInspirationRes(
        R.string.mood_insp_5_title,
        R.string.mood_insp_5_prompt,
        "05 / DRAMA",
        Color(0xFF4C0519).copy(alpha = 0.65f),
        Color(0xFF2A030E).copy(alpha = 0.40f),
        Color(0xFFF43F5E).copy(alpha = 0.25f),
        Color(0xFFFB7185)
    ),
    MoodInspirationRes(
        R.string.mood_insp_6_title,
        R.string.mood_insp_6_prompt,
        "06 / CYBER",
        Color(0xFF134E4A).copy(alpha = 0.65f),
        Color(0xFF3B0764).copy(alpha = 0.40f),
        Color(0xFF2DD4BF).copy(alpha = 0.30f),
        Color(0xFF2DD4BF)
    ),
    MoodInspirationRes(
        R.string.mood_insp_7_title,
        R.string.mood_insp_7_prompt,
        "07 / SEA",
        Color(0xFF083344).copy(alpha = 0.65f),
        Color(0xFF0E2A38).copy(alpha = 0.40f),
        Color(0xFF06B6D4).copy(alpha = 0.25f),
        Color(0xFF22D3EE)
    ),
    MoodInspirationRes(
        R.string.mood_insp_8_title,
        R.string.mood_insp_8_prompt,
        "08 / CRIME",
        Color(0xFF292524).copy(alpha = 0.75f),
        Color(0xFF451A03).copy(alpha = 0.40f),
        Color(0xFFD97706).copy(alpha = 0.25f),
        Color(0xFFF59E0B)
    )
)

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
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.search_mood_badge_label).uppercase(),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.40f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = rtlSafeLetterSpacing(1.2.sp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Griglia 2x4 delle 8 atmosfere consigliate con gradienti cinematografici (zero emoji)
        val chunkedItems = MOOD_INSPIRATION_ITEMS.chunked(2)
        chunkedItems.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { item ->
                    val title = stringResource(item.titleRes)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(72.dp)
                            .bounceClick(scaleDown = 0.95f, enabled = !isLoading) {
                                onMoodSelected(title)
                            }
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(item.startColor, item.endColor)
                                )
                            )
                            .border(1.dp, item.borderColor, RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        contentAlignment = Alignment.TopStart
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = title,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 15.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f)
                            )
                            Text(
                                text = item.tagText,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = rtlSafeLetterSpacing(1.sp),
                                color = item.tagColor
                            )
                        }
                    }
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (!errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
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
                    color = Color(0xFFFF8A8A)
                )
            }
        }
    }
}
