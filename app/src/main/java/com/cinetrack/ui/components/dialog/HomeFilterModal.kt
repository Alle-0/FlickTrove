package com.cinetrack.ui.components.dialog
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import com.cinetrack.R

import androidx.compose.ui.res.vectorResource
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.lerp as geometryLerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.cinetrack.data.model.GenreConstants
import com.cinetrack.data.model.SortConfig
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.components.shared.ModalCloseButton
import com.cinetrack.ui.components.shared.MorphGlassModal
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.theme.DarkSurface
import com.cinetrack.ui.utils.ProviderConstants
import com.cinetrack.ui.utils.bounceClick
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeChild
import com.cinetrack.ui.utils.verticalFadingEdges
import com.cinetrack.ui.utils.horizontalFadingEdges
import kotlin.math.roundToInt

@OptIn(ExperimentalAnimationApi::class, ExperimentalLayoutApi::class)
@Composable
fun HomeFilterModal(
    isVisible: Boolean,
    isVisti: Boolean = false,
    isFolder: Boolean = false,
    isCollectionFilter: Boolean = false,
    hasTvSeries: Boolean = false,
    sortConfig: SortConfig,
    hazeState: HazeState?,
    triggerBounds: Rect? = null,
    category: String = "movie",
    isCommentsFilter: Boolean = false,
    availableSources: List<com.cinetrack.data.api.SourceCatalogRow> = emptyList(),
    availableLanguages: List<String> = emptyList(),
    availableCountries: List<String> = emptyList(),
    sourceCounts: Map<String, Int> = emptyMap(),
    languageCounts: Map<String, Int> = emptyMap(),
    showSortBy: Boolean = true,
    suggestedFilters: List<com.cinetrack.ui.viewmodel.FilterPill> = emptyList(),
    initialKeywordName: String? = null,
    onSortConfigChanged: (SortConfig) -> Unit,
    onDismissRequest: () -> Unit
) {
    var expandedSection by remember(isVisible) { mutableStateOf<String?>(null) }
    var localSortConfig by remember(isVisible) { mutableStateOf(sortConfig) }
    var showSeriesStatusInfoDialog by remember { mutableStateOf(false) }

    // Sync local state when modal becomes visible
    LaunchedEffect(isVisible) {
        if (isVisible) {
            localSortConfig = sortConfig
            expandedSection = null
            showSeriesStatusInfoDialog = false
        }
    }

    MorphGlassModal(
        isVisible = isVisible,
        onDismissRequest = onDismissRequest,
        triggerBounds = triggerBounds,
        hazeState = hazeState,
        targetMaxWidth = 440.dp,
        targetWidthFraction = 0.94f,
        maxModalHeightFraction = 0.85f,
        minModalHeight = if (isCollectionFilter) (if (hasTvSeries) 320.dp else 260.dp) else 380.dp,
        defaultModalHeight = 380.dp,
        zIndex = 100f
    ) { contentAlpha ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(1f)
                .graphicsLayer(
                    alpha = contentAlpha,
                    compositingStrategy = CompositingStrategy.Offscreen
                )
        ) {
            // --- HEADER BAR ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 16.dp, top = 20.dp, bottom = 28.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                            val titleText = stringResource(R.string.filter_title)
                            Text(
                                text = titleText,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 3.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Reset All Button
                                val hasActiveFilters = if (isCollectionFilter) {
                                    localSortConfig.sortType != "chronological" ||
                                        localSortConfig.selectedMedia != null ||
                                        localSortConfig.sortDirection != "asc"
                                } else if (isCommentsFilter) {
                                    localSortConfig.sortType != "date" ||
                                        localSortConfig.sortDirection != "desc" ||
                                        localSortConfig.selectedSources.isNotEmpty() ||
                                        localSortConfig.selectedLanguages.isNotEmpty() ||
                                        localSortConfig.selectedSource != null ||
                                        localSortConfig.selectedLanguage != null
                                } else {
                                    localSortConfig.selectedGenres.isNotEmpty() ||
                                        localSortConfig.selectedCountries.isNotEmpty() ||
                                        localSortConfig.selectedKeywords.isNotEmpty() ||
                                        localSortConfig.selectedProviders.isNotEmpty() ||
                                        localSortConfig.selectedDecades.isNotEmpty() ||
                                        localSortConfig.selectedStatuses.isNotEmpty() ||
                                        localSortConfig.selectedSources.isNotEmpty() ||
                                        localSortConfig.selectedLanguages.isNotEmpty() ||
                                        localSortConfig.selectedSource != null ||
                                        localSortConfig.selectedLanguage != null
                                }
                                if (hasActiveFilters) {
                                    Row(
                                        modifier = Modifier
                                            .bounceClick {
                                                localSortConfig = if (isCollectionFilter) {
                                                    localSortConfig.copy(
                                                        sortType = "chronological",
                                                        sortDirection = "asc",
                                                        selectedMedia = null
                                                    )
                                                } else if (isCommentsFilter) {
                                                    localSortConfig.copy(
                                                        sortType = "date",
                                                        sortDirection = "desc",
                                                        selectedSources = emptyList(),
                                                        selectedLanguages = emptyList(),
                                                        selectedSource = null,
                                                        selectedLanguage = null
                                                    )
                                                } else {
                                                    localSortConfig.copy(
                                                        selectedGenres = emptyList(),
                                                        selectedCountries = emptyList(),
                                                        selectedKeywords = emptyList(),
                                                        selectedProviders = emptyList(),
                                                        selectedDecades = emptyList(),
                                                        selectedStatuses = emptyList(),
                                                        selectedSources = emptyList(),
                                                        selectedLanguages = emptyList(),
                                                        selectedSource = null,
                                                        selectedLanguage = null
                                                    )
                                                }
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
                                            text = stringResource(R.string.filter_reset),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary,
                                            letterSpacing = 1.sp,
                                            modifier = Modifier
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                ModalCloseButton(
                                    onClick = onDismissRequest
                                )
                            }
                        }

                        // --- SCROLLABLE CONTENT ---
                        val filterScrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .verticalFadingEdges(filterScrollState, 16.dp, 16.dp)
                                .verticalScroll(filterScrollState)
                                .padding(bottom = 12.dp)
                        ) {
                            // --- SORT SECTION ---
                            if (showSortBy) {
                                FilterSortSection(
                                    category = category,
                                    isCollectionFilter = isCollectionFilter,
                                    isCommentsFilter = isCommentsFilter,
                                    isVisti = isVisti,
                                    isFolder = isFolder,
                                    sortConfig = localSortConfig,
                                    isExpanded = expandedSection == "sort",
                                    onToggle = { expandedSection = if (expandedSection == "sort") null else "sort" },
                                    onInfoClick = { showSeriesStatusInfoDialog = true },
                                    onSortConfigChanged = { localSortConfig = it }
                                )
                            }

                            // --- SOURCE SECTION (Only for comments) ---
                            if (isCommentsFilter) {
                                FilterCommentsSourceSection(
                                    availableSources = availableSources,
                                    sourceCounts = sourceCounts,
                                    sortConfig = localSortConfig,
                                    isExpanded = expandedSection == "source",
                                    onToggle = { expandedSection = if (expandedSection == "source") null else "source" },
                                    onSortConfigChanged = { localSortConfig = it }
                                )
                            }

                            // --- LANGUAGE SECTION (Only for comments) ---
                            if (isCommentsFilter) {
                                FilterCommentsLanguageSection(
                                    availableLanguages = availableLanguages,
                                    languageCounts = languageCounts,
                                    sortConfig = localSortConfig,
                                    isExpanded = expandedSection == "language",
                                    onToggle = { expandedSection = if (expandedSection == "language") null else "language" },
                                    onSortConfigChanged = { localSortConfig = it }
                                )
                            }

                            // --- MEDIA TYPE SECTION (Only for collections with TV series) ---
                            if (isCollectionFilter && hasTvSeries) {
                                FilterMediaTypeSection(
                                    sortConfig = localSortConfig,
                                    isExpanded = expandedSection == "media",
                                    onToggle = { expandedSection = if (expandedSection == "media") null else "media" },
                                    onSortConfigChanged = { localSortConfig = it }
                                )
                            }

                            // --- STATUS SECTION (Only for TV, not for upcoming) ---
                            if (!isCommentsFilter && !isCollectionFilter && category.contains("tv") && !category.contains("upcoming")) {
                                FilterTvStatusSection(
                                    isVisti = isVisti,
                                    sortConfig = localSortConfig,
                                    isExpanded = expandedSection == "status",
                                    onToggle = { expandedSection = if (expandedSection == "status") null else "status" },
                                    onSortConfigChanged = { localSortConfig = it }
                                )
                            }

                            // --- ACTIVE KEYWORDS SECTION ---
                            if (!isCommentsFilter && !isCollectionFilter && localSortConfig.selectedKeywords.isNotEmpty()) {
                                FilterKeywordsSection(
                                    suggestedFilters = suggestedFilters,
                                    initialKeywordName = initialKeywordName,
                                    sortConfig = localSortConfig,
                                    isExpanded = expandedSection == "keywords" || expandedSection == null,
                                    onToggle = { expandedSection = if (expandedSection == "keywords") null else "keywords" },
                                    onSortConfigChanged = { localSortConfig = it }
                                )
                            }

                            // --- GENRES SECTION ---
                            if (!isCommentsFilter && !isCollectionFilter) {
                                FilterGenresSection(
                                    category = category,
                                    sortConfig = localSortConfig,
                                    isExpanded = expandedSection == "genres",
                                    onToggle = { expandedSection = if (expandedSection == "genres") null else "genres" },
                                    onSortConfigChanged = { localSortConfig = it },
                                    isFolder = isFolder,
                                    hasTvSeries = hasTvSeries
                                )
                            }

                            // --- PLATFORMS SECTION ---
                            if (!isCommentsFilter && !isCollectionFilter) {
                                FilterPlatformsSection(
                                    sortConfig = localSortConfig,
                                    isExpanded = expandedSection == "platforms",
                                    onToggle = { expandedSection = if (expandedSection == "platforms") null else "platforms" },
                                    onSortConfigChanged = { localSortConfig = it }
                                )
                            }

                            // --- COUNTRIES SECTION ---
                            if (!isCommentsFilter && !isCollectionFilter) {
                                FilterCountriesSection(
                                    sortConfig = localSortConfig,
                                    isExpanded = expandedSection == "countries",
                                    onToggle = { expandedSection = if (expandedSection == "countries") null else "countries" },
                                    onSortConfigChanged = { localSortConfig = it },
                                    availableCountries = availableCountries
                                )
                            }

                            // --- DECADES / RELEASE MONTHS SECTION ---
                            if (!isCommentsFilter && !isCollectionFilter) {
                                FilterPeriodSection(
                                    category = category,
                                    sortConfig = localSortConfig,
                                    isExpanded = expandedSection == "period",
                                    onToggle = { expandedSection = if (expandedSection == "period") null else "period" },
                                    onSortConfigChanged = { localSortConfig = it }
                                )
                            }
                        }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 24.dp, top = 8.dp)
                            .height(56.dp)
                            .bounceClick {
                                onSortConfigChanged(localSortConfig)
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

                // --- SERIES STATUS INFO DIALOG (root level for proper hazeGlass) ---
                if (showSeriesStatusInfoDialog) {
                    SeriesStatusInfoDialog(
                        hazeState = hazeState,
                        onDismiss = { showSeriesStatusInfoDialog = false }
                    )
                }
            }
}
