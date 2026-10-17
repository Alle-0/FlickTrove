package com.cinetrack.ui.components.badge

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.ui.theme.FlickTroveTheme
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.util.VibrationHelper

// ════════════════════════════════════════════════════════════════════
// VISTA DETTAGLIO HERO & ANTEPRIMA CATALOGO TROFEI
// ════════════════════════════════════════════════════════════════════


@Composable
fun BadgeHeroDetailShowcase(
    badge: PreviewBadgeItem,
    tier: PreviewBadgeTier,
    isUnlocked: Boolean = true,
    modifier: Modifier = Modifier
) {
    val isProgressive = badge.category == BadgeTypeCategory.PROGRESSIVE_TIERS
    val isMastered = isProgressive && tier == PreviewBadgeTier.THE_FINAL_CUT && isUnlocked

    val activeDescription = if (!isUnlocked && badge.secretHint != null) {
        badge.localizedSecretHint() ?: badge.localizedDescription()
    } else {
        badge.localizedDescription()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF151822),
                        Color(0xFF0D0F15)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        tier.primaryColor.copy(alpha = 0.45f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(32.dp)
            )
            .padding(22.dp)
    ) {
        // Tag di rarità globale
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(tier.primaryColor.copy(alpha = 0.10f))
                .border(
                    width = 1.dp,
                    color = tier.primaryColor.copy(alpha = 0.35f),
                    shape = CircleShape
                )
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = stringResource(R.string.trophy_rarity_pct, badge.rarityPercent).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.3.sp
                ),
                color = tier.primaryColor
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Emblema Grande Cine-Lens: PUREZZA CROMATICA SENZA DOPPI COLORI IBRIDI
        FlickTroveBadgeEmblem(
            tier = tier,
            iconKind = badge.iconKind,
            isUnlocked = isUnlocked,
            size = 130.dp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Formato e nome tier
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(tier.primaryColor)
            )
            Text(
                text = tier.localizedFormatName().uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.6.sp
                ),
                color = tier.primaryColor
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Titolo Grande Trofeo
        Text(
            text = badge.localizedTitle(),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 0.3.sp
            ),
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Descrizione d'onore cinefila dinamica
        Text(
            text = activeDescription,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp,
                lineHeight = 19.sp
            ),
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Timeline o Sigillo
        when (badge.category) {
            BadgeTypeCategory.PROGRESSIVE_TIERS -> {
                val nextTier = when (tier) {
                    PreviewBadgeTier.SUPER_8 -> PreviewBadgeTier.MM_16
                    PreviewBadgeTier.MM_16 -> PreviewBadgeTier.MM_35
                    PreviewBadgeTier.MM_35 -> PreviewBadgeTier.MM_70
                    PreviewBadgeTier.MM_70 -> PreviewBadgeTier.THE_FINAL_CUT
                    else -> null
                }
                val nextTarget = nextTier?.let { badge.progressiveSteps[it]?.targetThreshold }
                val stepFraction = if (nextTarget != null && nextTarget > 0) {
                    (badge.progressCurrent.toFloat() / nextTarget).coerceIn(0f, 1f)
                } else if (isUnlocked && tier == PreviewBadgeTier.THE_FINAL_CUT) 1f else 0f

                val maxStepTarget = badge.progressiveSteps[PreviewBadgeTier.THE_FINAL_CUT]?.targetThreshold ?: badge.progressTarget
                BadgeTierEvolutionTrack(
                    currentTier = tier,
                    isUnlocked = isUnlocked,
                    progressCurrent = badge.progressCurrent,
                    maxTarget = maxStepTarget,
                    stepProgressFraction = stepFraction,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            BadgeTypeCategory.DAY_ONE_HONOR -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(tier.primaryColor.copy(alpha = 0.10f))
                        .border(1.dp, tier.primaryColor.copy(alpha = 0.28f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "ONORIFICENZA STORICA DEI FONDATORI",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.4.sp
                            ),
                            color = tier.primaryColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Traguardo unico non replicabile • Registrato prima del 29 Agosto 2026",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color(0xFFE2E8F0),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            BadgeTypeCategory.SPECIAL_ACHIEVEMENT -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(tier.primaryColor.copy(alpha = 0.10f))
                        .border(1.dp, tier.primaryColor.copy(alpha = 0.28f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "TRAGUARDO CINEMATOGRAFICO D'AUTORE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.4.sp
                            ),
                            color = tier.primaryColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Formato d'eccellenza ${tier.formatName} • Riconoscimento speciale non progressivo",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color(0xFFE2E8F0),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            BadgeTypeCategory.LOST_REEL_SECRET -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF8B5CF6).copy(alpha = 0.12f))
                        .border(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.30f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "BOBINA PERDUTA • TRAGUARDO SEGRETO",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.4.sp
                            ),
                            color = Color(0xFFA78BFA)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isUnlocked) "Easter Egg scoperto! Condividilo con la community" else "Indizio misterioso celato tra le pieghe della cineteca...",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color(0xFFE2E8F0),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// 5. HUB COMPLETO DI ANTEPRIMA & SELEZIONE BADGE
// ════════════════════════════════════════════════════════════════════

@Composable
fun BadgePreviewShowcase() {
    var selectedBadge by remember { mutableStateOf(OFFICIAL_BADGES_CATALOG[0]) }
    var selectedProgressiveTier by remember { mutableStateOf(PreviewBadgeTier.THE_FINAL_CUT) }
    var isUnlocked by remember { mutableStateOf(true) }

    val activeTier = selectedBadge.fixedTier ?: selectedProgressiveTier

    Surface(
        color = Color(0xFF090B10),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(18.dp)
        ) {
            // Header
            Text(
                text = "FLICKTROVE • TROPHY SHOWCASE",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                ),
                color = Color(0xFF2DD4BF)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Anteprima Badge & Trofei",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black
                ),
                color = Color.White
            )
            Text(
                text = "Catalogo ufficiale con traguardi progressivi e trofei unici / Lost Reel.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // SELETTORE DEL TIPO DI BADGE
            Text(
                text = "SCEGLI UN BADGE DAL CATALOGO:",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = Color(0xFF64748B)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OFFICIAL_BADGES_CATALOG.forEach { badgeItem ->
                    val isSelected = badgeItem.id == selectedBadge.id
                    Box(
                        modifier = Modifier
                            .bounceClick {
                                selectedBadge = badgeItem
                                if (badgeItem.fixedTier == null) {
                                    selectedProgressiveTier = badgeItem.initialProgressiveTier
                                }
                            }
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color(0xFF2DD4BF).copy(alpha = 0.18f)
                                else Color.White.copy(alpha = 0.05f)
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) Color(0xFF2DD4BF) else Color.White.copy(alpha = 0.10f),
                                shape = CircleShape
                            )
                            .padding(horizontal = 13.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = badgeItem.title,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.5.sp
                            ),
                            color = if (isSelected) Color(0xFF2DD4BF) else Color(0xFF94A3B8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // SELETTORE DEL FORMATO TIER PER I PROGRESSIVI
            if (selectedBadge.category == BadgeTypeCategory.PROGRESSIVE_TIERS) {
                Text(
                    text = "CAMBIA FORMATO DEL BADGE PROGRESSIVO:",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.height(8.dp))

                val progressiveTiers = listOf(
                    PreviewBadgeTier.SUPER_8,
                    PreviewBadgeTier.MM_16,
                    PreviewBadgeTier.MM_35,
                    PreviewBadgeTier.MM_70,
                    PreviewBadgeTier.THE_FINAL_CUT
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    progressiveTiers.forEach { tier ->
                        val isCurrent = tier == selectedProgressiveTier
                        Box(
                            modifier = Modifier
                                .bounceClick { selectedProgressiveTier = tier }
                                .clip(CircleShape)
                                .background(
                                    if (isCurrent) tier.primaryColor.copy(alpha = 0.22f)
                                    else Color.White.copy(alpha = 0.05f)
                                )
                                .border(
                                    width = if (isCurrent) 1.5.dp else 1.dp,
                                    color = if (isCurrent) tier.primaryColor else Color.White.copy(alpha = 0.12f),
                                    shape = CircleShape
                                )
                                .padding(horizontal = 13.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = tier.shortLabel,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.sp
                                ),
                                color = if (isCurrent) tier.primaryColor else Color(0xFF94A3B8)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (selectedBadge.category == BadgeTypeCategory.LOST_REEL_SECRET)
                            "🗝️ Badge Segreto: Formato fisso Lost Reel (Nessuna progressione a livelli)"
                        else
                            "✨ Traguardo Storico: Formato d'onore The Final Cut (Non ha livelli intermedi)",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Color(0xFF94A3B8)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // TOGGLE SBLOCCATO / BLOCCATO
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .bounceClick { isUnlocked = true }
                        .clip(CircleShape)
                        .background(if (isUnlocked) Color(0xFF10B981).copy(alpha = 0.2f) else Color.Transparent)
                        .border(1.dp, if (isUnlocked) Color(0xFF10B981) else Color.White.copy(alpha = 0.12f), CircleShape)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "SBLOCCATO ✨",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isUnlocked) Color(0xFF10B981) else Color(0xFF94A3B8)
                    )
                }

                Box(
                    modifier = Modifier
                        .bounceClick { isUnlocked = false }
                        .clip(CircleShape)
                        .background(if (!isUnlocked) Color(0xFF64748B).copy(alpha = 0.2f) else Color.Transparent)
                        .border(1.dp, if (!isUnlocked) Color(0xFF64748B) else Color.White.copy(alpha = 0.12f), CircleShape)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "BLOCCATO 🔒",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (!isUnlocked) Color(0xFFE2E8F0) else Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 1. VISTA DETTAGLIO HERO & STORY 9:16
            Text(
                text = "1. VISTA DETTAGLIO & CONDIVISIONE STORY (9:16)",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                ),
                color = Color(0xFFE2E8F0)
            )
            Spacer(modifier = Modifier.height(10.dp))

            BadgeHeroDetailShowcase(
                badge = selectedBadge,
                tier = activeTier,
                isUnlocked = isUnlocked
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 2. CARD GRIGLIA TROFEI (SALA DEI TROFEI)
            Text(
                text = "2. CARD GRIGLIA TROFEI (VARI TIPI A CONFRONTO)",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                ),
                color = Color(0xFFE2E8F0)
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OFFICIAL_BADGES_CATALOG.forEach { sampleItem ->
                    val itemTier = sampleItem.fixedTier ?: sampleItem.initialProgressiveTier
                    TrophyBadgeCard(
                        badge = sampleItem,
                        tier = itemTier,
                        isUnlocked = sampleItem.id != "secret_groundhog_day" || isUnlocked,
                        onClick = {
                            selectedBadge = sampleItem
                            if (sampleItem.fixedTier == null) {
                                selectedProgressiveTier = sampleItem.initialProgressiveTier
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 3. PALETTE CROMATICA DEI 6 FORMATI
            Text(
                text = "3. PALETTE CROMATICA DEI 6 FORMATI (COLORI PURI COERENTI)",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                ),
                color = Color(0xFFE2E8F0)
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PreviewBadgeTier.values().forEach { tierItem ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(98.dp)
                            .bounceClick {
                                if (selectedBadge.category == BadgeTypeCategory.PROGRESSIVE_TIERS && tierItem != PreviewBadgeTier.LOST_REEL) {
                                    selectedProgressiveTier = tierItem
                                }
                            }
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF13151F))
                            .border(1.dp, tierItem.primaryColor.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                            .padding(10.dp)
                    ) {
                        FlickTroveBadgeEmblem(
                            tier = tierItem,
                            iconKind = selectedBadge.iconKind,
                            isUnlocked = true,
                            size = 54.dp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = tierItem.shortLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = tierItem.primaryColor,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// -------------------------------------------------------------
// PREVIEW ANDROID STUDIO
// -------------------------------------------------------------

@Preview(
    name = "Showcase Completo Trofei FlickTrove",
    showBackground = true,
    backgroundColor = 0xFF090B10,
    device = "spec:width=411dp,height=891dp"
)
@Composable
private fun BadgePreviewShowcaseFullPreview() {
    FlickTroveTheme {
        BadgePreviewShowcase()
    }
}

@Preview(
    name = "Card Frequenza 24fps (The Final Cut - 100% Completato)",
    showBackground = true,
    backgroundColor = 0xFF090B10
)
@Composable
private fun BadgeCardMasteredPreview() {
    FlickTroveTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            TrophyBadgeCard(
                badge = OFFICIAL_BADGES_CATALOG[0],
                tier = PreviewBadgeTier.THE_FINAL_CUT,
                isUnlocked = true
            )
        }
    }
}

@Preview(
    name = "Timeline Formati 16mm (Tick sul nodo attivo)",
    showBackground = true,
    backgroundColor = 0xFF090B10
)
@Composable
private fun BadgeTimeline16mmPreview() {
    FlickTroveTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            BadgeTierEvolutionTrack(
                currentTier = PreviewBadgeTier.MM_16,
                isUnlocked = true,
                progressCurrent = 25,
                maxTarget = 250,
                stepProgressFraction = 0.5f
            )
        }
    }
}
