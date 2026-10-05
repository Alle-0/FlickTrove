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
import com.cinetrack.util.buildTmdbImageUrl
import com.cinetrack.util.ImageType
import com.cinetrack.util.LocalImageQuality
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
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.cinetrack.data.model.GenreConstants
import com.cinetrack.data.model.SortConfig
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.components.shared.ModalCloseButton
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
    showSortBy: Boolean = true,
    suggestedFilters: List<com.cinetrack.ui.viewmodel.FilterPill> = emptyList(),
    initialKeywordName: String? = null,
    onSortConfigChanged: (SortConfig) -> Unit,
    onDismissRequest: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }

    val targetWidth = (screenWidth * 0.94f).coerceAtMost(with(density) { 450.dp.toPx() })
    
    var contentHeightPx by remember { mutableStateOf(0f) }
    val topSafetyPx = with(density) { 96.dp.toPx() }
    val bottomSafetyPx = with(density) { 56.dp.toPx() }
    val maxAllowedHeight = screenHeight - topSafetyPx - bottomSafetyPx
    
    var expandedSection by remember(isVisible) { mutableStateOf<String?>(null) }
    var showAllGenres by remember { mutableStateOf(false) }
    
    val minAllowedHeight = with(density) {
        when {
            isCollectionFilter -> (if (hasTvSeries) 320.dp else 260.dp).toPx()
            isCommentsFilter -> 380.dp.toPx()
            else -> 380.dp.toPx()
        }
    }

    val targetHeightPx by animateFloatAsState(
        targetValue = if (contentHeightPx > 0) contentHeightPx.coerceIn(minAllowedHeight, maxAllowedHeight) 
                      else screenHeight * 0.45f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "dynamicHeight"
    )

    val targetRect = Rect(
        left = (screenWidth - targetWidth) / 2f,
        top = (screenHeight - targetHeightPx) / 2f,
        right = (screenWidth + targetWidth) / 2f,
        bottom = (screenHeight + targetHeightPx) / 2f
    )

    val transition = updateTransition(targetState = isVisible, label = "FilterModalTransition")

    androidx.activity.compose.BackHandler(enabled = isVisible) {
        onDismissRequest()
    }

    val progress by transition.animateFloat(
        transitionSpec = {
            if (initialState == false && targetState == true) {
                spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioLowBouncy)
            } else {
                tween(durationMillis = 220, easing = FastOutSlowInEasing)
            }
        },
        label = "expansionProgress"
    ) { state -> if (state) 1f else 0f }

    val alpha by transition.animateFloat(label = "scrimAlpha") { state -> if (state) 1f else 0f }

    var localSortConfig by remember(isVisible) { mutableStateOf(sortConfig) }
    
    // Sync local state when modal becomes visible
    LaunchedEffect(isVisible) {
        if (isVisible) {
            localSortConfig = sortConfig
            expandedSection = null
        }
    }

    if ((transition.currentState || transition.targetState) && (isVisible || progress > 0.02f)) {
        val effectiveScrimAlpha = if (triggerBounds != null) {
            val scrimProgress = ((progress - 0.08f) / 0.92f).coerceIn(0f, 1f)
            0.6f * FastOutSlowInEasing.transform(scrimProgress)
        } else {
            0.6f * alpha
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(100f)
                .background(Color.Black.copy(alpha = effectiveScrimAlpha))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest
                )
        ) {
            // --- GHOST MEASUREMENT LAYER ---
            // This invisible Box measures the content's natural height without being clipped 
            // by the animated modal size, avoiding a measurement deadlock.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(0f)
                    .onSizeChanged { size: androidx.compose.ui.unit.IntSize ->
                        if (size.height > 0) contentHeightPx = size.height.toFloat()
                    }
            ) {
                // We only need to measure the items that dictate height
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                    Spacer(modifier = Modifier.height(20.dp + 28.dp + 32.dp)) // Header guestimate
                    
                    // Simple representation of the list to measure its natural growth
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                    ) {
                        // Measure based on which section is expanded
                        val expandedHeight = when(expandedSection) {
                            "sort" -> with(density) { (if (isCommentsFilter || isCollectionFilter) 48 * 2 + 100 else 48 * 6 + 100).dp.toPx() } // Approx sort items
                            "source" -> with(density) { ((availableSources.size.coerceAtLeast(2) + 1) * 44 + 30).dp.toPx() }
                            "language" -> with(density) { (11 * 44 + 30).dp.toPx().coerceAtMost(maxAllowedHeight * 0.50f) }
                            "media" -> with(density) { (48 * 3 + 30).dp.toPx() }
                            "genres" -> with(density) { 250.dp.toPx() }
                            "platforms" -> with(density) { 100.dp.toPx() }
                            "period" -> with(density) { 120.dp.toPx() }
                            "status" -> with(density) { 120.dp.toPx() }
                            else -> 0f
                        }
                        
                        Spacer(modifier = Modifier.height(
                            with(density) { 
                                val sectionCount = when {
                                    isCollectionFilter -> if (hasTvSeries) 2 else 1
                                    isCommentsFilter -> 3
                                    else -> 4
                                }
                                (54 * sectionCount).dp + // Section headers
                                expandedHeight.pxToDp(density) +
                                100.dp // Apply button space
                            }
                        ))
                    }
                }
            }

            val startRect = triggerBounds ?: targetRect

            // Center moves smoothly towards screen center
            val currentCenterX = lerp(startRect.center.x, targetRect.center.x, progress)
            val currentCenterY = lerp(startRect.center.y, targetRect.center.y, progress)

            // Delayed size expansion: stays compact/circular during liftoff (0..0.18), then blossoms into full size
            val sizeProgress = if (triggerBounds != null) {
                val delay = 0.18f
                if (progress <= delay) {
                    (progress / delay) * 0.05f
                } else {
                    val raw = (progress - delay) / (1f - delay)
                    0.05f + 0.95f * FastOutSlowInEasing.transform(raw.coerceIn(0f, 1f))
                }
            } else {
                lerp(0.92f, 1f, progress)
            }

            // Corner radius stays circular during liftoff, then morphs into target rounded corners
            val cornerRadiusProgress = if (triggerBounds != null) {
                val delay = 0.18f
                if (progress <= delay) 0f
                else FastOutSlowInEasing.transform(((progress - delay) / (1f - delay)).coerceIn(0f, 1f))
            } else {
                1f
            }

            val currentWidth = if (triggerBounds != null) lerp(startRect.width, targetRect.width, sizeProgress) else targetRect.width * sizeProgress
            val currentHeight = if (triggerBounds != null) lerp(startRect.height, targetRect.height, sizeProgress) else targetRect.height * sizeProgress

            val currentRect = Rect(
                left = currentCenterX - currentWidth / 2f,
                top = currentCenterY - currentHeight / 2f,
                right = currentCenterX + currentWidth / 2f,
                bottom = currentCenterY + currentHeight / 2f
            )

            val startRadius = if (triggerBounds != null) startRect.width / 2f else with(density) { 32.dp.toPx() }
            val endRadius = with(density) { 32.dp.toPx() }
            val currentCornerRadius = lerp(startRadius, endRadius, cornerRadiusProgress)
            val currentShape = RoundedCornerShape(with(density) { currentCornerRadius.toDp() })

            val currentTintAlpha = if (triggerBounds != null) {
                lerp(0.48f, HazeStyles.glassmorphicDialog.tint.alpha, sizeProgress)
            } else {
                HazeStyles.glassmorphicDialog.tint.alpha
            }
            val currentBlur = if (triggerBounds != null) {
                androidx.compose.ui.unit.lerp(16.dp, HazeStyles.glassmorphicDialog.blurRadius, sizeProgress)
            } else {
                HazeStyles.glassmorphicDialog.blurRadius
            }
            val animatedStyle = remember(currentTintAlpha, currentBlur) {
                HazeStyles.glassmorphicDialog.copy(
                    tint = HazeStyles.glassmorphicDialog.tint.copy(alpha = currentTintAlpha),
                    blurRadius = currentBlur
                )
            }
            val borderAlpha = if (triggerBounds != null) {
                lerp(HazeStyles.ModalBorderAlphaStart, HazeStyles.ModalBorderAlpha, sizeProgress)
            } else {
                HazeStyles.ModalBorderAlpha
            }
            val modalAlpha = if (triggerBounds != null) {
                if (progress <= 0.06f) 0f
                else ((progress - 0.06f) / 0.24f).coerceIn(0f, 1f)
            } else {
                if (progress <= 0.02f) 0f else progress
            }

            Box(
                modifier = Modifier
                    .offset { IntOffset(currentRect.left.roundToInt(), currentRect.top.roundToInt()) }
                    .size(
                        width = with(density) { currentRect.width.toDp() },
                        height = with(density) { currentRect.height.toDp() }
                    )
                    .graphicsLayer { this.alpha = modalAlpha }
                    .bounceClick(scaleDown = 1f) { /* Prevent dismissal */ }
            ) {
                // Background Layer (Blurred glass)
                Spacer(
                    modifier = Modifier
                        .matchParentSize()
                        .hazeGlass(
                            state = hazeState,
                            shape = currentShape,
                            style = animatedStyle,
                            alpha = modalAlpha,
                            useOffscreenStrategy = false
                        )
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = borderAlpha),
                            shape = currentShape
                        )
                )

                // Foreground Content
                if (progress > 0.38f) {
                    val contentAlpha = ((progress - 0.38f) / 0.62f).coerceIn(0f, 1f)

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .animateContentSize()
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
                            val titleText = if (isCollectionFilter) {
                                stringResource(R.string.collection_filter_modal_title).uppercase()
                            } else {
                                stringResource(R.string.filter_title)
                            }
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
                                } else {
                                    localSortConfig.selectedGenres.isNotEmpty() ||
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
                                                } else {
                                                    localSortConfig.copy(
                                                        selectedGenres = emptyList(),
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
                                .weight(1f)
                                .verticalFadingEdges(filterScrollState, 16.dp, 16.dp)
                                .verticalScroll(filterScrollState)
                                .padding(bottom = 12.dp)
                        ) {
                            // --- SORT SECTION ---
                            if (showSortBy) {
                                ExpandableSection(
                                    title = stringResource(R.string.filter_sort_by),
                                    isExpanded = expandedSection == "sort",
                                    showChevron = true,
                                    isClickable = true,
                                    onToggle = { expandedSection = if (expandedSection == "sort") null else "sort" }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val isDiscoverCategory = category.contains("popular") ||
                                            category.contains("upcoming") ||
                                            category.contains("now_playing") ||
                                            category.contains("airing") ||
                                            category.contains("trending") ||
                                            category.contains("streaming") ||
                                            category.contains("on_the_air") ||
                                            category == "genre"

                                        val sortOptions = buildList {
                                            if (isCollectionFilter) {
                                                add(FilterOption("chronological", stringResource(R.string.collection_filter_sort_chronological)))
                                                add(FilterOption("release_date", stringResource(R.string.filter_sort_release_date)))
                                            } else if (isCommentsFilter) {
                                                add(FilterOption("date", stringResource(R.string.comment_sort_date)))
                                                add(FilterOption("likes", stringResource(R.string.comment_sort_likes)))
                                            } else if (isDiscoverCategory) {
                                                if (category.contains("upcoming")) {
                                                    add(FilterOption("release_date", stringResource(R.string.filter_sort_release_date)))
                                                    add(FilterOption("created_at", stringResource(R.string.person_popularity).lowercase().replaceFirstChar { it.uppercase() }))
                                                } else {
                                                    add(FilterOption("created_at", stringResource(R.string.person_popularity).lowercase().replaceFirstChar { it.uppercase() }))
                                                    add(FilterOption("release_date", stringResource(R.string.filter_sort_release_date)))
                                                    add(FilterOption("personal_rating", stringResource(R.string.detail_rating)))
                                                    add(FilterOption("title", stringResource(R.string.filter_sort_title)))
                                                }
                                            } else {
                                                if (isVisti) {
                                                    add(FilterOption("last_watched_at", stringResource(R.string.filter_sort_last_watched_at)))
                                                    add(FilterOption("first_watched_at", stringResource(R.string.filter_sort_first_watched_at)))
                                                    add(FilterOption("rewatch_count", stringResource(R.string.filter_sort_rewatch_count)))
                                                }
                                                if (isFolder) {
                                                    add(FilterOption("manual", stringResource(R.string.filter_sort_manual)))
                                                }
                                                add(FilterOption("added_at", stringResource(R.string.filter_sort_added_at)))
                                                add(FilterOption("release_date", stringResource(R.string.filter_sort_release_date)))
                                                add(FilterOption("title", stringResource(R.string.filter_sort_title)))
                                                add(FilterOption("personal_rating", stringResource(R.string.filter_sort_personal_rating)))
                                                add(FilterOption("runtime", stringResource(R.string.filter_sort_runtime)))
                                                if (category != "movie") {
                                                    add(FilterOption("remaining_episodes", stringResource(R.string.filter_sort_remaining_episodes)))
                                                }
                                            }
                                        }

                                        sortOptions.forEach { option ->
                                            SortOptionItem(
                                                label = option.label,
                                                isSelected = localSortConfig.sortType == option.id,
                                                onClick = { localSortConfig = localSortConfig.copy(sortType = option.id) }
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
                                                isSelected = localSortConfig.sortDirection == "desc",
                                                icon = ImageVector.vectorResource(id = R.drawable.ic_right),
                                                iconRotation = 90f,
                                                modifier = Modifier.weight(1f),
                                                onClick = { localSortConfig = localSortConfig.copy(sortDirection = "desc") }
                                            )
                                            DirectionChip(
                                                label = stringResource(R.string.filter_dir_asc),
                                                isSelected = localSortConfig.sortDirection == "asc",
                                                icon = ImageVector.vectorResource(id = R.drawable.ic_right),
                                                iconRotation = -90f,
                                                modifier = Modifier.weight(1f),
                                                onClick = { localSortConfig = localSortConfig.copy(sortDirection = "asc") }
                                            )
                                        }
                                    }
                                }
                            }

                            // --- SOURCE SECTION (Only for comments) ---
                            if (isCommentsFilter) {
                                val activeSources = remember(localSortConfig.selectedSources, localSortConfig.selectedSource) {
                                    (localSortConfig.selectedSources + listOfNotNull(localSortConfig.selectedSource)).toSet()
                                }
                                ExpandableSection(
                                    title = stringResource(R.string.filter_source),
                                    isExpanded = expandedSection == "source",
                                    showChevron = true,
                                    isClickable = true,
                                    badgeCount = activeSources.size,
                                    onToggle = { expandedSection = if (expandedSection == "source") null else "source" }
                                ) {
                                    val sources = remember(availableSources) {
                                        val rawList = if (availableSources.isNotEmpty()) {
                                            availableSources
                                        } else {
                                            listOf(
                                                com.cinetrack.data.api.SourceCatalogRow(slug = "flicktrove", displayName = "FlickTrove"),
                                                com.cinetrack.data.api.SourceCatalogRow(slug = "tvtime", displayName = "TV Time Refugees")
                                            )
                                        }
                                        val flickTroveItem = rawList.firstOrNull { it.slug.equals("flicktrove", ignoreCase = true) }
                                            ?: com.cinetrack.data.api.SourceCatalogRow(slug = "flicktrove", displayName = "FlickTrove")
                                        val otherItems = rawList
                                            .filter { !it.slug.equals("flicktrove", ignoreCase = true) }
                                            .sortedBy { (it.displayName?.ifBlank { it.slug } ?: it.slug).lowercase() }
                                        listOf(flickTroveItem) + otherItems
                                    }
                                    FlowRow(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilterChip(
                                            label = stringResource(R.string.filter_source_all),
                                            isSelected = activeSources.isEmpty(),
                                            onClick = { localSortConfig = localSortConfig.copy(selectedSources = emptyList(), selectedSource = null) }
                                        )
                                        sources.forEach { source ->
                                            val isSelected = source.slug in activeSources
                                            FilterChip(
                                                label = source.displayName?.ifBlank { source.slug } ?: source.slug,
                                                isSelected = isSelected,
                                                onClick = {
                                                    val newSet = if (isSelected) activeSources - source.slug else activeSources + source.slug
                                                    localSortConfig = localSortConfig.copy(
                                                        selectedSources = newSet.toList(),
                                                        selectedSource = null
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // --- LANGUAGE SECTION (Only for comments) ---
                            if (isCommentsFilter) {
                                val activeLanguages = remember(localSortConfig.selectedLanguages, localSortConfig.selectedLanguage) {
                                    (localSortConfig.selectedLanguages + listOfNotNull(localSortConfig.selectedLanguage)).toSet()
                                }
                                ExpandableSection(
                                    title = stringResource(R.string.filter_language),
                                    isExpanded = expandedSection == "language",
                                    showChevron = true,
                                    isClickable = true,
                                    badgeCount = activeLanguages.size,
                                    onToggle = { expandedSection = if (expandedSection == "language") null else "language" }
                                ) {
                                    val currentLocale = if (!configuration.locales.isEmpty) configuration.locales[0] else java.util.Locale.getDefault()
                                    val commsUniSupportedLanguageCodes = remember {
                                        listOf(
                                            "en", "it", "es", "fr", "de", "pt", "ru", "tr", "ar", "pl",
                                            "nl", "id", "ja", "ko", "zh", "hi", "el", "hu", "cs", "ro",
                                            "sv", "da", "fi", "no", "uk", "vi", "th", "he", "fa", "ms",
                                            "bg", "hr", "sr", "sk"
                                        )
                                    }
                                    val allLanguageCodes = remember(availableLanguages, commsUniSupportedLanguageCodes) {
                                        (commsUniSupportedLanguageCodes + availableLanguages)
                                            .map { it.lowercase().trim() }
                                            .filter { it.isNotBlank() }
                                            .distinct()
                                    }
                                    val allLabel = stringResource(R.string.flow_filter_media_all)
                                    val languages = remember(currentLocale, allLanguageCodes, allLabel) {
                                        val userLangCode = currentLocale.language.lowercase()
                                        val mapped = allLanguageCodes.map { code ->
                                            val loc = java.util.Locale.forLanguageTag(code)
                                            val rawName = loc.getDisplayLanguage(currentLocale)
                                            val displayName = rawName.replaceFirstChar {
                                                if (it.isLowerCase()) it.titlecase(currentLocale) else it.toString()
                                            }.ifBlank { code.uppercase() }
                                            code to displayName
                                        }
                                        val userLangPair = mapped.firstOrNull { it.first == userLangCode }
                                        val otherLangs = mapped
                                            .filter { it.first != userLangCode }
                                            .sortedBy { it.second.lowercase(currentLocale) }
                                        if (userLangPair != null) {
                                            listOf(userLangPair) + otherLangs
                                        } else {
                                            otherLangs
                                        }
                                    }
                                    FlowRow(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilterChip(
                                            label = allLabel,
                                            isSelected = activeLanguages.isEmpty(),
                                            onClick = { localSortConfig = localSortConfig.copy(selectedLanguages = emptyList(), selectedLanguage = null) }
                                        )
                                        languages.forEach { (code, label) ->
                                            val isSelected = code in activeLanguages
                                            FilterChip(
                                                label = label,
                                                isSelected = isSelected,
                                                onClick = {
                                                    val newSet = if (isSelected) activeLanguages - code else activeLanguages + code
                                                    localSortConfig = localSortConfig.copy(
                                                        selectedLanguages = newSet.toList(),
                                                        selectedLanguage = null
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // --- MEDIA TYPE SECTION (Only for collections with TV series) ---
                            if (isCollectionFilter && hasTvSeries) {
                                ExpandableSection(
                                    title = stringResource(R.string.flow_filter_section_media),
                                    isExpanded = expandedSection == "media",
                                    showChevron = true,
                                    isClickable = true,
                                    badgeCount = if (localSortConfig.selectedMedia != null) 1 else 0,
                                    onToggle = { expandedSection = if (expandedSection == "media") null else "media" }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        SortOptionItem(
                                            label = stringResource(R.string.flow_filter_media_all),
                                            isSelected = localSortConfig.selectedMedia == null,
                                            onClick = { localSortConfig = localSortConfig.copy(selectedMedia = null) }
                                        )
                                        SortOptionItem(
                                            label = stringResource(R.string.flow_filter_media_movies),
                                            isSelected = localSortConfig.selectedMedia == "movie",
                                            onClick = { localSortConfig = localSortConfig.copy(selectedMedia = "movie") }
                                        )
                                        SortOptionItem(
                                            label = stringResource(R.string.flow_filter_media_tv),
                                            isSelected = localSortConfig.selectedMedia == "tv",
                                            onClick = { localSortConfig = localSortConfig.copy(selectedMedia = "tv") }
                                        )
                                    }
                                }
                            }

                            // --- STATUS SECTION (Only for TV, not for upcoming) ---
                            if (!isCommentsFilter && !isCollectionFilter && category.contains("tv") && !category.contains("upcoming")) {
                                ExpandableSection(
                                    title = stringResource(R.string.filter_status),
                                    isExpanded = expandedSection == "status",
                                    badgeCount = localSortConfig.selectedStatuses.size,
                                    onToggle = { expandedSection = if (expandedSection == "status") null else "status" }
                                ) {
                                    val statuses = if (!isVisti) {
                                        listOf(
                                            "watching" to stringResource(R.string.filter_status_watching),
                                            "not_started" to stringResource(R.string.filter_status_not_started),
                                            "dropped" to stringResource(R.string.filter_status_dropped)
                                        )
                                    } else {
                                        listOf(
                                            "up_to_date" to stringResource(R.string.filter_status_up_to_date)
                                        )
                                    }
                                    
                                    FlowRow(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        statuses.forEach { (statusId, label) ->
                                            val isSelected = statusId in localSortConfig.selectedStatuses
                                            FilterChip(
                                                label = label.uppercase(),
                                                isSelected = isSelected,
                                                onClick = {
                                                    val newList = if (isSelected) localSortConfig.selectedStatuses - statusId
                                                               else localSortConfig.selectedStatuses + statusId
                                                    localSortConfig = localSortConfig.copy(selectedStatuses = newList)
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // --- ACTIVE KEYWORDS SECTION ---
                            if (!isCommentsFilter && !isCollectionFilter && localSortConfig.selectedKeywords.isNotEmpty()) {
                                ExpandableSection(
                                    title = stringResource(R.string.filter_active_subgenres),
                                    isExpanded = expandedSection == "keywords" || expandedSection == null,
                                    badgeCount = localSortConfig.selectedKeywords.size,
                                    onToggle = { expandedSection = if (expandedSection == "keywords") null else "keywords" }
                                ) {
                                    FlowRow(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        localSortConfig.selectedKeywords.forEach { keywordId ->
                                            val currentLanguage = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]?.language ?: "en"
                                            val dictName = com.cinetrack.data.model.KeywordDictionary.getLocalizedKeywordName(keywordId, currentLanguage)?.uppercase()
                                            
                                            val suggestedName = suggestedFilters.find { it.id == keywordId }?.name?.uppercase()
                                            
                                            val keywordName = suggestedName ?: dictName ?: initialKeywordName?.uppercase() ?: stringResource(R.string.filter_selected_subgenre)
                                                
                                            FilterChip(
                                                label = keywordName,
                                                isSelected = true,
                                                onClick = {
                                                    localSortConfig = localSortConfig.copy(selectedKeywords = localSortConfig.selectedKeywords - keywordId)
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // --- GENRES SECTION ---
                            if (!isCommentsFilter && !isCollectionFilter) {
                                ExpandableSection(
                                title = stringResource(R.string.filter_genres),
                                isExpanded = expandedSection == "genres",
                                badgeCount = localSortConfig.selectedGenres.size,
                                onToggle = { expandedSection = if (expandedSection == "genres") null else "genres" }
                            ) {
                                FlowRow(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val relevantGenres = if (category.contains("tv")) GenreConstants.TV_GENRES else GenreConstants.MOVIE_GENRES
                                    val displayedGenres = if (showAllGenres) {
                                        relevantGenres
                                    } else {
                                        // Show first 10 genres OR any selected genres
                                        relevantGenres.filterIndexed { index, genre ->
                                            index < 10 || genre.id in localSortConfig.selectedGenres
                                        }
                                    }

                                    displayedGenres.forEach { genre ->
                                        val isSelected = genre.id in localSortConfig.selectedGenres
                                        val currentLanguage = LocalConfiguration.current.locales[0]?.language ?: "en"
                                        val localizedName = GenreConstants.getLocalizedName(genre.id, currentLanguage, genre.name)
                                        FilterChip(
                                            label = localizedName.uppercase(),
                                            isSelected = isSelected,
                                            onClick = {
                                                val newList = if (isSelected) localSortConfig.selectedGenres - genre.id
                                                           else localSortConfig.selectedGenres + genre.id
                                                localSortConfig = localSortConfig.copy(selectedGenres = newList)
                                            }
                                        )
                                    }
                                    
                                    if (!showAllGenres && relevantGenres.size > displayedGenres.size) {
                                        Box(
                                            modifier = Modifier
                                                .bounceClick { showAllGenres = true }
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.45f))
                                                .border(width = 1.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), shape = CircleShape)
                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_plus),
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = stringResource(R.string.filter_show_all).uppercase(),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                                )
                                            }
                                        }
                                    }
                                    }
                                }
                            }

                            // --- PLATFORMS SECTION ---
                            if (!isCommentsFilter && !isCollectionFilter) {
                                ExpandableSection(
                                title = stringResource(R.string.filter_platforms),
                                isExpanded = expandedSection == "platforms",
                                badgeCount = localSortConfig.selectedProviders.size,
                                onToggle = { expandedSection = if (expandedSection == "platforms") null else "platforms" }
                            ) {
                                val currentLang = configuration.locales[0]?.language?.lowercase() ?: java.util.Locale.getDefault().language.lowercase()
                                val availableProviders = remember(currentLang) { ProviderConstants.getAvailableProviders(currentLang) }
                                val providersListState = androidx.compose.foundation.lazy.rememberLazyListState()
                                LazyRow(
                                    state = providersListState,
                                    modifier = Modifier.horizontalFadingEdges(providersListState, leftEdgeWidth = 16.dp, rightEdgeWidth = 16.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(availableProviders, key = { it.providerId }, contentType = { "provider" }) { provider ->
                                        val isSelected = provider.providerId in localSortConfig.selectedProviders
                                        ProviderItem(
                                            name = provider.providerName,
                                            logoPath = provider.logoPath,
                                            isSelected = isSelected,
                                            onClick = {
                                                val newList = if (isSelected)
                                                    localSortConfig.selectedProviders - provider.providerId
                                                else
                                                    localSortConfig.selectedProviders + provider.providerId
                                                localSortConfig = localSortConfig.copy(selectedProviders = newList)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // --- DECADES / RELEASE MONTHS SECTION ---
                        if (!isCommentsFilter && !isCollectionFilter) {
                            val isUpcoming = category.contains("upcoming")
                            val sectionTitle = if (isUpcoming) {
                                stringResource(R.string.filter_release_month)
                            } else {
                                stringResource(R.string.filter_period)
                            }
                            ExpandableSection(
                                title = sectionTitle,
                                isExpanded = expandedSection == "period",
                                badgeCount = localSortConfig.selectedDecades.size,
                                onToggle = { expandedSection = if (expandedSection == "period") null else "period" }
                            ) {
                                if (isUpcoming) {
                                    val currentYearMonth = remember { java.time.YearMonth.now() }
                                    val currentLocale = configuration.locales[0] ?: java.util.Locale.getDefault()
                                    val months = remember(currentYearMonth, currentLocale) {
                                        (0..11).map { offset ->
                                            val ym = currentYearMonth.plusMonths(offset.toLong())
                                            val key = String.format(java.util.Locale.US, "%04d-%02d", ym.year, ym.monthValue)
                                            val rawName = ym.month.getDisplayName(java.time.format.TextStyle.FULL, currentLocale)
                                            val capitalized = rawName.replaceFirstChar {
                                                if (it.isLowerCase()) it.titlecase(currentLocale) else it.toString()
                                            }
                                            val label = if (ym.year == currentYearMonth.year) {
                                                capitalized
                                            } else {
                                                "$capitalized '${ym.year % 100}"
                                            }
                                            key to label
                                        }
                                    }
                                    val beyondLabel = stringResource(R.string.filter_future_beyond)

                                    FlowRow(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        months.forEach { (key, label) ->
                                            val isSelected = key in localSortConfig.selectedDecades
                                            FilterChip(
                                                label = label,
                                                isSelected = isSelected,
                                                onClick = {
                                                    val newList = if (isSelected) localSortConfig.selectedDecades - key
                                                    else localSortConfig.selectedDecades + key
                                                    localSortConfig = localSortConfig.copy(selectedDecades = newList)
                                                }
                                            )
                                        }
                                        val isBeyondSelected = "future" in localSortConfig.selectedDecades
                                        FilterChip(
                                            label = beyondLabel,
                                            isSelected = isBeyondSelected,
                                            onClick = {
                                                val newList = if (isBeyondSelected) localSortConfig.selectedDecades - "future"
                                                else localSortConfig.selectedDecades + "future"
                                                localSortConfig = localSortConfig.copy(selectedDecades = newList)
                                            }
                                        )
                                    }
                                } else {
                                    val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                                    val currentDecade = (currentYear / 10) * 10
                                    val decades = (currentDecade downTo 1960 step 10).map { it.toString() }
                                    FlowRow(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        decades.forEach { decade ->
                                            val isSelected = decade in localSortConfig.selectedDecades
                                            FilterChip(
                                                label = "${decade}s",
                                                isSelected = isSelected,
                                                onClick = {
                                                    val newList = if (isSelected) localSortConfig.selectedDecades - decade
                                                    else localSortConfig.selectedDecades + decade
                                                    localSortConfig = localSortConfig.copy(selectedDecades = newList)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
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
                }
            }
        }
    }
}

// Helper for Dp conversions inside the Composable scope
private fun Float.pxToDp(density: androidx.compose.ui.unit.Density) = with(density) { this@pxToDp.toDp() }
