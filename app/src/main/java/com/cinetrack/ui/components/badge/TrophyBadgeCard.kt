package com.cinetrack.ui.components.badge

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.ui.utils.bounceClick

// 3. CARD DA VETRINA PER LA GRIGLIA TROFEI (TROPHY SHOWCASE)
// ════════════════════════════════════════════════════════════════════

@Composable
fun TrophyBadgeCard(
    badge: PreviewBadgeItem,
    tier: PreviewBadgeTier,
    isUnlocked: Boolean = true,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val borderColor = if (isUnlocked) {
        tier.primaryColor.copy(alpha = 0.32f)
    } else {
        Color.White.copy(alpha = 0.08f)
    }

    val isProgressive = badge.category == BadgeTypeCategory.PROGRESSIVE_TIERS

    // Formati ordinati della pellicola cinematografica
    val tierSequence = listOf(
        PreviewBadgeTier.SUPER_8,
        PreviewBadgeTier.MM_16,
        PreviewBadgeTier.MM_35,
        PreviewBadgeTier.MM_70,
        PreviewBadgeTier.THE_FINAL_CUT
    )

    // Formati supportati da questo specifico badge (es. per i generi solo MM_16 e MM_70)
    val configuredTiers = tierSequence.filter { it in badge.progressiveSteps }
    val currentIdx = configuredTiers.indexOf(tier)

    val nextTier = if (!isUnlocked) {
        configuredTiers.firstOrNull()
    } else if (currentIdx >= 0 && currentIdx < configuredTiers.size - 1) {
        configuredTiers[currentIdx + 1]
    } else {
        null
    }

    val isMastered = isUnlocked && (isProgressive && (nextTier == null || configuredTiers.isEmpty()))

    val targetThreshold = if (!isUnlocked) {
        nextTier?.let { badge.progressiveSteps[it]?.targetThreshold } ?: badge.progressTarget
    } else if (nextTier != null) {
        badge.progressiveSteps[nextTier]?.targetThreshold ?: badge.progressTarget
    } else {
        badge.progressiveSteps[tier]?.targetThreshold ?: badge.progressTarget
    }

    val progressFraction = when {
        !isUnlocked -> 0f
        isMastered -> 1f
        isProgressive && targetThreshold > 0 -> (badge.progressCurrent.toFloat() / targetThreshold).coerceIn(0f, 1f)
        else -> 1f
    }

    Box(
        modifier = modifier
            .widthIn(min = 104.dp)
            .height(148.dp)
            .bounceClick { onClick() }
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 6.dp, vertical = 10.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(2.dp))

            // Emblema Cinematico FlickTrove con Progress Pill circolare integrata
            FlickTroveBadgeEmblem(
                tier = tier,
                iconKind = badge.iconKind,
                isUnlocked = isUnlocked,
                progressFraction = progressFraction,
                showProgressRing = true,
                size = 66.dp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Slot fisso per il contatore x/n SOPRA AL TITOLO (15.dp):
            // garantisce perfetta simmetria e la stessa identica altezza su tutte le card della griglia.
            val unitStr = badge.localizedUnit()
            val progressCountText = if (isProgressive && !isMastered) {
                if (unitStr.isNotBlank()) {
                    "${badge.progressCurrent} / $targetThreshold $unitStr".trim()
                } else {
                    "${badge.progressCurrent} / $targetThreshold"
                }
            } else if (isProgressive && isMastered) {
                if (unitStr.isNotBlank()) {
                    "${badge.progressCurrent} $unitStr".trim()
                } else {
                    "${badge.progressCurrent}"
                }
            } else {
                null
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(15.dp),
                contentAlignment = Alignment.Center
            ) {
                if (progressCountText != null) {
                    Text(
                        text = progressCountText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 9.5.sp,
                            fontWeight = if (isUnlocked) FontWeight.SemiBold else FontWeight.Medium,
                            letterSpacing = 0.2.sp
                        ),
                        color = if (isUnlocked) tier.primaryColor else Color(0xFF94A3B8),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            // Titolo Trofeo con altezza fissa (30.dp) per allineare a registro tutte le card (1 o 2 righe)
            // Padding orizzontale di 8.dp per garantire margine laterale elegante e mandare a capo prima i titoli lunghi
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = badge.localizedTitle(),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        lineHeight = 13.5.sp
                    ),
                    color = if (isUnlocked) Color.White else Color(0xFF94A3B8),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
