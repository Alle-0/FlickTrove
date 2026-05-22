package com.cinetrack.ui.components

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
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.cinetrack.data.GenreConstants
import com.cinetrack.data.models.SortConfig
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.utils.ProviderConstants
import com.cinetrack.ui.utils.bounceClick
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeChild
import kotlin.math.roundToInt

@OptIn(ExperimentalAnimationApi::class, ExperimentalLayoutApi::class)
@Composable
fun HomeFilterModal(
    isVisible: Boolean,
    isVisti: Boolean = false,
    sortConfig: SortConfig,
    hazeState: HazeState?,
    triggerBounds: Rect? = null,
    category: String = "movie",
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
    
    var expandedSection by remember { mutableStateOf<String?>(null) }
    var showAllGenres by remember { mutableStateOf(false) }
    
    val targetHeightPx by animateFloatAsState(
        targetValue = if (contentHeightPx > 0) contentHeightPx.coerceIn(with(density) { 380.dp.toPx() }, maxAllowedHeight) 
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

    val progress by transition.animateFloat(
        transitionSpec = {
            if (initialState == false && targetState == true) {
                spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioLowBouncy)
            } else {
                spring(stiffness = Spring.StiffnessMedium)
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
        }
    }

    if (transition.currentState || transition.targetState) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(100f)
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
                            "sort" -> with(density) { (48 * 6 + 100).dp.toPx() } // Approx sort items
                            "genres" -> with(density) { 250.dp.toPx() }
                            "platforms" -> with(density) { 100.dp.toPx() }
                            "period" -> with(density) { 120.dp.toPx() }
                            else -> 0f
                        }
                        
                        Spacer(modifier = Modifier.height(
                            with(density) { 
                                (54 * 4).dp + // 4 Section headers
                                expandedHeight.pxToDp(density) +
                                100.dp // Apply button space
                            }
                        ))
                    }
                }
            }

            val startRect = triggerBounds ?: targetRect.copy(
                left = targetRect.center.x - 20f,
                top = targetRect.center.y - 20f,
                right = targetRect.center.x + 20f,
                bottom = targetRect.center.y + 20f
            )

            val currentRect = geometryLerp(startRect, targetRect, progress)
            val currentCornerRadius = lerp(
                if (triggerBounds != null) startRect.width / 2f else with(density) { 32.dp.toPx() },
                with(density) { 32.dp.toPx() },
                progress
            )

            Box(
                modifier = Modifier
                    .offset { IntOffset(currentRect.left.roundToInt(), currentRect.top.roundToInt()) }
                    .size(
                        width = with(density) { currentRect.width.toDp() },
                        height = with(density) { currentRect.height.toDp() }
                    )
                    .bounceClick(scaleDown = 1f) { /* Prevent dismissal */ }
            ) {
                // Background Layer (Blurred glass)
                Spacer(
                    modifier = Modifier
                        .matchParentSize()
                        .hazeGlass(
                            state = hazeState,
                            shape = RoundedCornerShape(currentCornerRadius.pxToDp(density)),
                            useOffscreenStrategy = true,
                            alpha = alpha
                        )
                )

                // Foreground Content
                if (progress > 0.4f) {
                    val contentAlpha = ((progress - 0.4f) / 0.6f).coerceIn(0f, 1f)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                            .zIndex(1f)
                            .graphicsLayer(
                                alpha = contentAlpha
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
                            Text(
                                text = "FILTRI",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 3.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Reset All Button
                                val hasActiveFilters = localSortConfig.selectedGenres.isNotEmpty() ||
                                    localSortConfig.selectedProviders.isNotEmpty() ||
                                    localSortConfig.selectedDecades.isNotEmpty()
                                if (hasActiveFilters) {
                                    Row(
                                        modifier = Modifier
                                            .bounceClick {
                                                localSortConfig = localSortConfig.copy(
                                                    selectedGenres = emptyList(),
                                                    selectedProviders = emptyList(),
                                                    selectedDecades = emptyList()
                                                )
                                            }
                                            .clip(RoundedCornerShape(24.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = HazeStyles.GlassAlphaLow))
                                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = HazeStyles.GlassAlphaMedium), RoundedCornerShape(24.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.RestartAlt,
                                            contentDescription = "Reset filtri",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "RESET",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary,
                                            letterSpacing = 1.sp,
                                            modifier = Modifier
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .bounceClick { onDismissRequest() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Chiudi",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // --- SCROLLABLE CONTENT ---
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(bottom = 12.dp)
                        ) {
                            // --- SORT SECTION ---
                            ExpandableSection(
                                title = "ORDINA PER",
                                isExpanded = expandedSection == "sort",
                                onToggle = { expandedSection = if (expandedSection == "sort") null else "sort" }
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val sortOptions = buildList {
                                        if (isVisti) {
                                            add(FilterOption("watched_at", "Data di visione"))
                                        }
                                        add(FilterOption("added_at", "Aggiunti di recente"))
                                        add(FilterOption("release_date", "Data di uscita"))
                                        add(FilterOption("title", "Titolo alfabetico"))
                                        add(FilterOption("personal_rating", "Voto personale"))
                                        add(FilterOption("runtime", "Durata totale"))
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
                                            label = "Decrescente",
                                            isSelected = localSortConfig.sortDirection == "desc",
                                            icon = Icons.Rounded.KeyboardArrowDown,
                                            modifier = Modifier.weight(1f),
                                            onClick = { localSortConfig = localSortConfig.copy(sortDirection = "desc") }
                                        )
                                        DirectionChip(
                                            label = "Crescente",
                                            isSelected = localSortConfig.sortDirection == "asc",
                                            icon = Icons.Rounded.KeyboardArrowUp,
                                            modifier = Modifier.weight(1f),
                                            onClick = { localSortConfig = localSortConfig.copy(sortDirection = "asc") }
                                        )
                                    }
                                }
                            }

                            // --- GENRES SECTION ---
                            ExpandableSection(
                                title = "GENERI",
                                isExpanded = expandedSection == "genres",
                                badgeCount = localSortConfig.selectedGenres.size,
                                onToggle = { expandedSection = if (expandedSection == "genres") null else "genres" }
                            ) {
                                FlowRow(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val relevantGenres = if (category == "tv") GenreConstants.TV_GENRES else GenreConstants.MOVIE_GENRES
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
                                        FilterChip(
                                            label = genre.name.uppercase(),
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
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Add,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "MOSTRA TUTTI",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // --- PLATFORMS SECTION ---
                            ExpandableSection(
                                title = "PIATTAFORME",
                                isExpanded = expandedSection == "platforms",
                                badgeCount = localSortConfig.selectedProviders.size,
                                onToggle = { expandedSection = if (expandedSection == "platforms") null else "platforms" }
                            ) {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(ProviderConstants.ALL_PROVIDERS) { provider ->
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

                            // --- PERIOD SECTION ---
                            ExpandableSection(
                                title = "PERIODO",
                                isExpanded = expandedSection == "period",
                                badgeCount = localSortConfig.selectedDecades.size,
                                onToggle = { expandedSection = if (expandedSection == "period") null else "period" }
                            ) {
                                val decades = listOf("2020", "2010", "2000", "1990", "1980", "1970", "1960")
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
                                text = "APPLICA FILTRI",
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

@Composable
private fun ExpandableSection(
    title: String,
    isExpanded: Boolean,
    badgeCount: Int = 0,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    val rotation by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "arrowRotation")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .bounceClick { onToggle() }
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
                    )
                ),
                shape = RoundedCornerShape(28.dp)
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isExpanded) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        letterSpacing = 1.2.sp
                    )
                    
                    AnimatedVisibility(
                        visible = badgeCount > 0,
                        enter = fadeIn() + scaleIn(initialScale = 0.8f),
                        exit = fadeOut() + scaleOut(targetScale = 0.8f)
                    ) {
                        Row {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = badgeCount.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer(rotationZ = rotation)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Box(modifier = Modifier.padding(bottom = 14.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.Transparent,
        label = "chipBorder"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
        label = "chipBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
        label = "chipText"
    )

    Box(
        modifier = Modifier
            .bounceClick { onClick() }
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = textColor
        )
    }
}

@Composable
private fun ProviderItem(
    name: String,
    logoPath: String?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "providerBorder"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
        label = "providerBg"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(64.dp)
            .bounceClick { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(bgColor)
                .border(width = 2.dp, color = borderColor, shape = RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (logoPath != null) {
                SubcomposeAsyncImage(
                    model = ProviderConstants.getLogoUrl(logoPath),
                    contentDescription = name,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    loading = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White.copy(alpha = 0.05f))
                        )
                    },
                    error = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name.take(1).uppercase(),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }
                )
            } else {
                Text(
                    text = name.take(1).uppercase(),
                    color = Color.White.copy(alpha = 0.3f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = name,
            fontSize = 9.sp,
            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SortOptionItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .bounceClick { onClick() }
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
            .border(
                width = 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
            fontSize = 14.sp
        )

        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun DirectionChip(
    label: String,
    isSelected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f),
        label = "dirBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.45f),
        label = "dirContent"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else Color.Transparent,
        label = "dirBorder"
    )

    Box(
        modifier = modifier
            .height(40.dp)
            .bounceClick { onClick() }
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = contentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

private data class FilterOption(val id: String, val label: String)
