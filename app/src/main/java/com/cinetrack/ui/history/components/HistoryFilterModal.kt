package com.cinetrack.ui.history.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.cinetrack.R
import com.cinetrack.ui.components.dialog.ExpandableSection
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.cinetrack.ui.components.dialog.DirectionChip
import com.cinetrack.ui.components.dialog.FilterChip
import com.cinetrack.ui.components.dialog.SortOptionItem
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.components.shared.ModalCloseButton
import com.cinetrack.ui.history.model.HistoryFilterConfig
import com.cinetrack.ui.history.model.HistoryMediaTypeFilter
import com.cinetrack.ui.history.model.HistorySortOption
import com.cinetrack.ui.history.model.HistoryViewingTypeFilter
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.utils.bounceClick
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import com.cinetrack.ui.components.shared.MorphGlassModal
import com.cinetrack.ui.utils.verticalFadingEdges
import dev.chrisbanes.haze.HazeState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryFilterModal(
    isVisible: Boolean,
    filterConfig: HistoryFilterConfig,
    onFilterConfigChanged: (HistoryFilterConfig) -> Unit,
    onDismissRequest: () -> Unit,
    triggerBounds: Rect? = null,
    hazeState: HazeState? = null
) {
    var localConfig by remember(isVisible, filterConfig) { mutableStateOf(filterConfig) }
    var expandedSection by remember(isVisible) { mutableStateOf<String?>(null) }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            localConfig = filterConfig
            expandedSection = null
        }
    }

    MorphGlassModal(
        isVisible = isVisible,
        onDismissRequest = onDismissRequest,
        triggerBounds = triggerBounds,
        hazeState = hazeState,
        targetMaxWidth = 420.dp
    ) { contentAlpha ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
                .graphicsLayer { alpha = contentAlpha }
        ) {
            // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 16.dp, top = 20.dp, bottom = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.filter_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 3.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (localConfig.isFiltered) {
                            Row(
                                modifier = Modifier
                                    .bounceClick {
                                        localConfig = HistoryFilterConfig()
                                    }
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = HazeStyles.GlassAlphaLow))
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = HazeStyles.GlassAlphaMedium),
                                        RoundedCornerShape(24.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_ricarica),
                                    contentDescription = stringResource(R.string.filter_reset),
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
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        ModalCloseButton(onClick = onDismissRequest)
                    }
                }

                // Scrollable Accordion Sections
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalFadingEdges(scrollState, 16.dp, 16.dp)
                        .verticalScroll(scrollState)
                        .padding(bottom = 8.dp)
                ) {
                    // SECTION 1: ORDINA PER (Data sempre selezionata + Direzione Decrescente/Crescente)
                    ExpandableSection(
                        title = stringResource(R.string.filter_sort_by),
                        isExpanded = expandedSection == "sort",
                        showChevron = true,
                        isClickable = true,
                        badgeCount = if (localConfig.sortOption != HistorySortOption.DATE_DESC) 1 else 0,
                        onToggle = { expandedSection = if (expandedSection == "sort") null else "sort" }
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            SortOptionItem(
                                label = stringResource(R.string.comment_sort_date),
                                isSelected = true,
                                onClick = { /* Data è sempre l'ordinamento cardine del Diario */ }
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                DirectionChip(
                                    label = stringResource(R.string.filter_dir_desc),
                                    isSelected = localConfig.sortOption == HistorySortOption.DATE_DESC,
                                    icon = ImageVector.vectorResource(id = R.drawable.ic_right),
                                    iconRotation = 90f,
                                    modifier = Modifier.weight(1f),
                                    onClick = { localConfig = localConfig.copy(sortOption = HistorySortOption.DATE_DESC) }
                                )
                                DirectionChip(
                                    label = stringResource(R.string.filter_dir_asc),
                                    isSelected = localConfig.sortOption == HistorySortOption.DATE_ASC,
                                    icon = ImageVector.vectorResource(id = R.drawable.ic_right),
                                    iconRotation = -90f,
                                    modifier = Modifier.weight(1f),
                                    onClick = { localConfig = localConfig.copy(sortOption = HistorySortOption.DATE_ASC) }
                                )
                            }
                        }
                    }

                    // SECTION 2: TIPO MEDIA (Pillole FilterChip)
                    ExpandableSection(
                        title = stringResource(R.string.history_filter_media_type),
                        isExpanded = expandedSection == "media",
                        showChevron = true,
                        isClickable = true,
                        badgeCount = if (localConfig.mediaTypeFilter != HistoryMediaTypeFilter.ALL) 1 else 0,
                        onToggle = { expandedSection = if (expandedSection == "media") null else "media" }
                    ) {
                        FlowRow(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                label = stringResource(R.string.history_filter_all),
                                isSelected = localConfig.mediaTypeFilter == HistoryMediaTypeFilter.ALL,
                                onClick = { localConfig = localConfig.copy(mediaTypeFilter = HistoryMediaTypeFilter.ALL) }
                            )
                            FilterChip(
                                label = stringResource(R.string.history_filter_movies),
                                isSelected = localConfig.mediaTypeFilter == HistoryMediaTypeFilter.MOVIES,
                                onClick = { localConfig = localConfig.copy(mediaTypeFilter = HistoryMediaTypeFilter.MOVIES) }
                            )
                            FilterChip(
                                label = stringResource(R.string.history_filter_tv),
                                isSelected = localConfig.mediaTypeFilter == HistoryMediaTypeFilter.TV,
                                onClick = { localConfig = localConfig.copy(mediaTypeFilter = HistoryMediaTypeFilter.TV) }
                            )
                        }
                    }

                    // SECTION 3: TIPO VISIONE (Pillole FilterChip)
                    ExpandableSection(
                        title = stringResource(R.string.history_filter_viewing_type),
                        isExpanded = expandedSection == "viewing",
                        showChevron = true,
                        isClickable = true,
                        badgeCount = if (localConfig.viewingTypeFilter != HistoryViewingTypeFilter.ALL) 1 else 0,
                        onToggle = { expandedSection = if (expandedSection == "viewing") null else "viewing" }
                    ) {
                        FlowRow(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                label = stringResource(R.string.history_filter_all_viewings),
                                isSelected = localConfig.viewingTypeFilter == HistoryViewingTypeFilter.ALL,
                                onClick = { localConfig = localConfig.copy(viewingTypeFilter = HistoryViewingTypeFilter.ALL) }
                            )
                            FilterChip(
                                label = stringResource(R.string.history_filter_rewatches_only),
                                isSelected = localConfig.viewingTypeFilter == HistoryViewingTypeFilter.REWATCH_ONLY,
                                onClick = { localConfig = localConfig.copy(viewingTypeFilter = HistoryViewingTypeFilter.REWATCH_ONLY) }
                            )
                            FilterChip(
                                label = stringResource(R.string.history_filter_first_watches_only),
                                isSelected = localConfig.viewingTypeFilter == HistoryViewingTypeFilter.FIRST_WATCH_ONLY,
                                onClick = { localConfig = localConfig.copy(viewingTypeFilter = HistoryViewingTypeFilter.FIRST_WATCH_ONLY) }
                            )
                        }
                    }

                    // SECTION 4: NOTE (Pillole FilterChip)
                    ExpandableSection(
                        title = stringResource(R.string.history_filter_notes),
                        isExpanded = expandedSection == "notes",
                        showChevron = true,
                        isClickable = true,
                        badgeCount = if (localConfig.withNotesOnly) 1 else 0,
                        onToggle = { expandedSection = if (expandedSection == "notes") null else "notes" }
                    ) {
                        FlowRow(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                label = stringResource(R.string.history_filter_all_notes),
                                isSelected = !localConfig.withNotesOnly,
                                onClick = { localConfig = localConfig.copy(withNotesOnly = false) }
                            )
                            FilterChip(
                                label = stringResource(R.string.history_filter_with_notes_only),
                                isSelected = localConfig.withNotesOnly,
                                onClick = { localConfig = localConfig.copy(withNotesOnly = true) }
                            )
                        }
                    }
                }

                // Apply Filters CTA Button (matching Screenshot 3 full width cyan CTA)
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(56.dp)
                        .bounceClick {
                            onFilterConfigChanged(localConfig)
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
