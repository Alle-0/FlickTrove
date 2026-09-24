package com.cinetrack.ui.components.dialog

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.ui.components.detail.ALL_VIBES
import com.cinetrack.ui.components.shared.ModalCloseButton
import com.cinetrack.ui.components.shared.MorphGlassModal
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.verticalFadingEdges
import com.cinetrack.ui.viewmodel.FlowFilterConfig
import com.cinetrack.ui.viewmodel.FlowMediaTypeFilter
import com.cinetrack.ui.viewmodel.FlowSortOption
import com.cinetrack.ui.viewmodel.FlowSortOrder
import com.cinetrack.ui.viewmodel.VibeStat
import dev.chrisbanes.haze.HazeState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowFilterModal(
    isVisible: Boolean,
    config: FlowFilterConfig,
    availableVibes: List<VibeStat>,
    totalCount: Int,
    hazeState: HazeState?,
    triggerBounds: Rect? = null,
    onApply: (FlowFilterConfig) -> Unit,
    onDismissRequest: () -> Unit
) {
    var localConfig by remember(isVisible) { mutableStateOf(config) }
    var isSortExpanded by remember(isVisible) { mutableStateOf(false) }
    var isVibeExpanded by remember(isVisible) { mutableStateOf(false) }
    var isMediaExpanded by remember(isVisible) { mutableStateOf(false) }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            localConfig = config
        }
    }

    MorphGlassModal(
        isVisible = isVisible,
        onDismissRequest = onDismissRequest,
        triggerBounds = triggerBounds,
        hazeState = hazeState,
        targetMaxWidth = 420.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 16.dp, top = 20.dp, bottom = 28.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.filter_title),
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
                                    localConfig = FlowFilterConfig()
                                }
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = HazeStyles.GlassAlphaLow))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = HazeStyles.GlassAlphaMedium), RoundedCornerShape(24.dp))
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
                                text = stringResource(id = R.string.filter_reset),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    ModalCloseButton(
                        onClick = onDismissRequest
                    )
                }
            }

            // Scrollable Sections
            val filterScrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalFadingEdges(filterScrollState, 16.dp, 16.dp)
                    .verticalScroll(filterScrollState),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Sort By Section
                ExpandableSection(
                    title = stringResource(id = R.string.filter_sort_by),
                    isExpanded = isSortExpanded,
                    showChevron = true,
                    isClickable = true,
                    onToggle = { isSortExpanded = !isSortExpanded }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SortOptionItem(
                            label = stringResource(id = R.string.flow_filter_sort_recent),
                            isSelected = localConfig.sortOption == FlowSortOption.RECENT_CHECKIN,
                            onClick = { localConfig = localConfig.copy(sortOption = FlowSortOption.RECENT_CHECKIN) }
                        )
                        SortOptionItem(
                            label = stringResource(id = R.string.flow_filter_sort_watch_date),
                            isSelected = localConfig.sortOption == FlowSortOption.WATCH_DATE,
                            onClick = { localConfig = localConfig.copy(sortOption = FlowSortOption.WATCH_DATE) }
                        )
                        SortOptionItem(
                            label = stringResource(id = R.string.flow_filter_sort_rating),
                            isSelected = localConfig.sortOption == FlowSortOption.RATING,
                            onClick = { localConfig = localConfig.copy(sortOption = FlowSortOption.RATING) }
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DirectionChip(
                                label = stringResource(id = R.string.filter_dir_desc),
                                icon = ImageVector.vectorResource(id = R.drawable.ic_right),
                                iconRotation = 90f,
                                isSelected = localConfig.sortOrder == FlowSortOrder.DESC,
                                modifier = Modifier.weight(1f),
                                onClick = { localConfig = localConfig.copy(sortOrder = FlowSortOrder.DESC) }
                            )
                            DirectionChip(
                                label = stringResource(id = R.string.filter_dir_asc),
                                icon = ImageVector.vectorResource(id = R.drawable.ic_right),
                                iconRotation = -90f,
                                isSelected = localConfig.sortOrder == FlowSortOrder.ASC,
                                modifier = Modifier.weight(1f),
                                onClick = { localConfig = localConfig.copy(sortOrder = FlowSortOrder.ASC) }
                            )
                        }
                    }
                }

                // 2. Emotional Vibe Section
                ExpandableSection(
                    title = stringResource(id = R.string.flow_filter_section_vibe),
                    isExpanded = isVibeExpanded,
                    showChevron = true,
                    isClickable = true,
                    badgeCount = localConfig.selectedVibes.size,
                    onToggle = { isVibeExpanded = !isVibeExpanded }
                ) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // All Vibes Chip
                        val isAllSelected = localConfig.selectedVibes.isEmpty()
                        Box(
                            modifier = Modifier
                                .bounceClick { localConfig = localConfig.copy(selectedVibes = emptySet()) }
                                .clip(CircleShape)
                                .background(if (isAllSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.45f))
                                .border(
                                    1.dp,
                                    if (isAllSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.12f),
                                    CircleShape
                                )
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.flow_filter_all_vibes),
                                color = if (isAllSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            )
                        }

                        // Specific Vibes
                        availableVibes.forEach { vibeStat ->
                            val isSelected = vibeStat.vibe in localConfig.selectedVibes
                            val emotionalVibe = ALL_VIBES.find { it.code == vibeStat.vibe }
                            val label = emotionalVibe?.labelRes?.let { stringResource(it) } ?: vibeStat.vibe
                            val color = emotionalVibe?.colorHex?.let { Color(it) } ?: MaterialTheme.colorScheme.primary

                            Box(
                                modifier = Modifier
                                    .bounceClick {
                                        val newVibes = if (isSelected) {
                                            localConfig.selectedVibes - vibeStat.vibe
                                        } else {
                                            localConfig.selectedVibes + vibeStat.vibe
                                        }
                                        localConfig = localConfig.copy(selectedVibes = newVibes)
                                    }
                                    .clip(CircleShape)
                                    .background(if (isSelected) color.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.45f))
                                    .border(
                                        1.dp,
                                        if (isSelected) color else Color.White.copy(alpha = 0.12f),
                                        CircleShape
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (vibeStat.iconRes != null) {
                                        Icon(
                                            painter = painterResource(id = vibeStat.iconRes),
                                            contentDescription = null,
                                            tint = if (isSelected) color else Color.White.copy(alpha = 0.7f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    } else {
                                        Text(text = vibeStat.emoji, fontSize = 12.sp)
                                    }
                                    Text(
                                        text = label,
                                        color = if (isSelected) color else Color.White.copy(alpha = 0.9f),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Media Type Section
                ExpandableSection(
                    title = stringResource(id = R.string.flow_filter_section_media),
                    isExpanded = isMediaExpanded,
                    showChevron = true,
                    isClickable = true,
                    badgeCount = if (localConfig.mediaType != FlowMediaTypeFilter.ALL) 1 else 0,
                    onToggle = { isMediaExpanded = !isMediaExpanded }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val mediaOptions = listOf(
                            FlowMediaTypeFilter.ALL to stringResource(id = R.string.flow_filter_media_all),
                            FlowMediaTypeFilter.MOVIE to stringResource(id = R.string.flow_filter_media_movies),
                            FlowMediaTypeFilter.TV to stringResource(id = R.string.flow_filter_media_tv)
                        )
                        mediaOptions.forEach { (type, label) ->
                            val isSelected = localConfig.mediaType == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .bounceClick { localConfig = localConfig.copy(mediaType = type) }
                                    .clip(CircleShape)
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.45f))
                                    .border(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.12f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Apply Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 12.dp)
                    .height(56.dp)
                    .bounceClick {
                        onApply(localConfig)
                    }
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.filter_apply),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}
