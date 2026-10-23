package com.cinetrack.ui.components.badge

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.ui.components.dialog.DirectionChip
import com.cinetrack.ui.components.dialog.ExpandableSection
import com.cinetrack.ui.components.dialog.FilterChip
import com.cinetrack.ui.components.dialog.SortOptionItem
import com.cinetrack.ui.components.shared.ModalCloseButton
import com.cinetrack.ui.components.shared.MorphGlassModal
import com.cinetrack.ui.theme.FlickTroveTheme
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.verticalFadingEdges
import dev.chrisbanes.haze.HazeState

/**
 * Modale di Filtro Autentico FlickTrove con MorphGlassModal (conforme alle regole 14 e 15 di GEMINI.md).
 * Si espande direttamente dal pulsante filtri in alto a destra e gestisce Categoria, Stato e Tier.
 */
@Composable
fun TrophyFilterModal(
    isVisible: Boolean,
    config: TrophyFilterConfig,
    triggerBounds: Rect?,
    hazeState: HazeState?,
    onApply: (TrophyFilterConfig) -> Unit,
    onDismissRequest: () -> Unit
) {
    var localConfig by remember(isVisible) { mutableStateOf(config) }
    var expandedSection by remember(isVisible) { mutableStateOf<String?>(null) }

    val categoryList = listOf(
        TrophyFilterCategory.ALL,
        TrophyFilterCategory.PROGRESSIVE,
        TrophyFilterCategory.GENRES_ERAS,
        TrophyFilterCategory.HONORS,
        TrophyFilterCategory.LOST_REEL
    )
    val statusList = listOf(
        TrophyStatusFilter.ALL,
        TrophyStatusFilter.UNLOCKED,
        TrophyStatusFilter.LOCKED
    )
    val tierList = listOf(
        null to R.string.trophy_cat_all,
        PreviewBadgeTier.LOST_REEL to R.string.trophy_format_lost_reel,
        PreviewBadgeTier.THE_FINAL_CUT to R.string.trophy_format_final_cut,
        PreviewBadgeTier.MM_70 to R.string.trophy_format_70mm,
        PreviewBadgeTier.MM_35 to R.string.trophy_format_35mm,
        PreviewBadgeTier.MM_16 to R.string.trophy_format_16mm,
        PreviewBadgeTier.SUPER_8 to R.string.trophy_format_super_8
    )

    MorphGlassModal(
        isVisible = isVisible,
        onDismissRequest = onDismissRequest,
        triggerBounds = triggerBounds,
        hazeState = hazeState,
        targetMaxWidth = 420.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // --- HEADER BAR (1:1 con HomeFilterModal) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 16.dp, top = 20.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.trophy_filter_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (localConfig.hasActiveFilters) {
                        Row(
                            modifier = Modifier
                                .bounceClick {
                                    localConfig = TrophyFilterConfig()
                                }
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_ricarica),
                                contentDescription = "Reset filtri",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = stringResource(R.string.filter_reset),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    ModalCloseButton(
                        onClose = onDismissRequest
                    )
                }
            }

            // --- SCROLLABLE CONTENT (Senza spaziatura forzata tra card: spacing nativo 4dp da ExpandableSection) ---
            val scrollState = rememberScrollState()
            val maxScrollHeight = (LocalConfiguration.current.screenHeightDp.dp * 0.52f).coerceAtLeast(240.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxScrollHeight)
                    .verticalFadingEdges(scrollState, 16.dp, 16.dp)
                    .verticalScroll(scrollState)
                    .padding(bottom = 12.dp)
            ) {
                // 1. ORDINA PER (Ufficiale FlickTrove con SortOptionItem e DirectionChip)
                ExpandableSection(
                    title = stringResource(R.string.trophy_filter_sort_by),
                    isExpanded = expandedSection == "sort",
                    showChevron = true,
                    isClickable = true,
                    badgeCount = if (localConfig.sortBy != TrophySortOption.TIER || localConfig.sortDirection != "desc") 1 else 0,
                    onToggle = { expandedSection = if (expandedSection == "sort") null else "sort" }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TrophySortOption.values().forEach { option ->
                            SortOptionItem(
                                label = stringResource(option.titleRes),
                                isSelected = localConfig.sortBy == option,
                                onClick = { localConfig = localConfig.copy(sortBy = option) }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DirectionChip(
                                label = stringResource(R.string.filter_dir_desc),
                                isSelected = localConfig.sortDirection == "desc",
                                icon = ImageVector.vectorResource(id = R.drawable.ic_right),
                                iconRotation = 90f,
                                modifier = Modifier.weight(1f),
                                onClick = { localConfig = localConfig.copy(sortDirection = "desc") }
                            )
                            DirectionChip(
                                label = stringResource(R.string.filter_dir_asc),
                                isSelected = localConfig.sortDirection == "asc",
                                icon = ImageVector.vectorResource(id = R.drawable.ic_right),
                                iconRotation = -90f,
                                modifier = Modifier.weight(1f),
                                onClick = { localConfig = localConfig.copy(sortDirection = "asc") }
                            )
                        }
                    }
                }

                // 2. CATEGORIA (Multi-selezione con FilterChip)
                ExpandableSection(
                    title = stringResource(R.string.trophy_filter_category),
                    isExpanded = expandedSection == "category",
                    showChevron = true,
                    isClickable = true,
                    badgeCount = if (localConfig.categories.contains(TrophyFilterCategory.ALL)) 0 else localConfig.categories.size,
                    onToggle = { expandedSection = if (expandedSection == "category") null else "category" }
                ) {
                    FlowRow(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categoryList.forEach { cat ->
                            val isAll = cat == TrophyFilterCategory.ALL
                            val isSelected = if (isAll) localConfig.categories.isEmpty() || TrophyFilterCategory.ALL in localConfig.categories else cat in localConfig.categories
                            val isLostReel = cat == TrophyFilterCategory.LOST_REEL
                            val lostReelColor = if (isLostReel) PreviewBadgeTier.LOST_REEL.primaryColor else null

                            FilterChip(
                                label = stringResource(cat.titleRes),
                                isSelected = isSelected,
                                onClick = {
                                    localConfig = if (isAll) {
                                        localConfig.copy(categories = emptySet())
                                    } else {
                                        val withoutAll = localConfig.categories - TrophyFilterCategory.ALL
                                        val newSet = if (isSelected) withoutAll - cat else withoutAll + cat
                                        localConfig.copy(categories = newSet)
                                    }
                                },
                                accentColor = lostReelColor ?: MaterialTheme.colorScheme.primary,
                                unselectedBorderColor = lostReelColor?.copy(alpha = 0.35f) ?: Color.Transparent,
                                unselectedBgColor = if (lostReelColor != null) lostReelColor.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.45f),
                                dotColor = lostReelColor
                            )
                        }
                    }
                }

                // 3. STATO (FilterChip ufficiali dell'app)
                ExpandableSection(
                    title = stringResource(R.string.trophy_filter_status),
                    isExpanded = expandedSection == "status",
                    showChevron = true,
                    isClickable = true,
                    badgeCount = if (localConfig.status != TrophyStatusFilter.ALL) 1 else 0,
                    onToggle = { expandedSection = if (expandedSection == "status") null else "status" }
                ) {
                    FlowRow(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        statusList.forEach { statusOption ->
                            FilterChip(
                                label = stringResource(statusOption.titleRes),
                                isSelected = localConfig.status == statusOption,
                                onClick = { localConfig = localConfig.copy(status = statusOption) }
                            )
                        }
                    }
                }

                // 4. FORMATO PELLICOLA (Multi-selezione con FilterChip)
                ExpandableSection(
                    title = stringResource(R.string.trophy_filter_format),
                    isExpanded = expandedSection == "tier",
                    showChevron = true,
                    isClickable = true,
                    badgeCount = localConfig.tiers.size,
                    onToggle = { expandedSection = if (expandedSection == "tier") null else "tier" }
                ) {
                    FlowRow(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tierList.forEach { (tierOption, labelRes) ->
                            val isAll = tierOption == null
                            val isSelected = if (isAll) localConfig.tiers.isEmpty() else tierOption in localConfig.tiers
                            val tierColor = tierOption?.primaryColor

                            FilterChip(
                                label = stringResource(labelRes),
                                isSelected = isSelected,
                                onClick = {
                                    localConfig = if (isAll) {
                                        localConfig.copy(tiers = emptySet())
                                    } else {
                                        val newSet = if (isSelected) localConfig.tiers - tierOption else localConfig.tiers + tierOption
                                        localConfig.copy(tiers = newSet)
                                    }
                                },
                                accentColor = tierColor ?: MaterialTheme.colorScheme.primary,
                                unselectedBorderColor = tierColor?.copy(alpha = 0.35f) ?: Color.Transparent,
                                unselectedBgColor = if (tierColor != null) tierColor.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.45f),
                                dotColor = tierColor
                            )
                        }
                    }
                }
            }

            // --- PULSANTE APPLICA (1:1 con HomeFilterModal) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp, top = 8.dp)
                    .height(56.dp)
                    .bounceClick {
                        onApply(localConfig)
                        onDismissRequest()
                    }
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.filter_apply),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}



@Preview(
    name = "Modale Filtro Trofei Ufficiale",
    showBackground = true,
    backgroundColor = 0xFF090A10,
    device = "spec:width=411dp,height=891dp"
)
@Composable
private fun TrophyFilterModalPreview() {
    FlickTroveTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            TrophyFilterModal(
                isVisible = true,
                config = TrophyFilterConfig(),
                triggerBounds = null,
                hazeState = null,
                onApply = {},
                onDismissRequest = {}
            )
        }
    }
}
