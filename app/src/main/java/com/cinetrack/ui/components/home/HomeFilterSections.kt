package com.cinetrack.ui.components.home

import com.cinetrack.ui.components.dialog.ExpandableSection
import com.cinetrack.ui.components.dialog.SortOptionItem
import com.cinetrack.ui.components.dialog.DirectionChip
import com.cinetrack.ui.components.dialog.FilterChip
import com.cinetrack.ui.components.dialog.FilterOption
import com.cinetrack.ui.components.dialog.SeriesStatusInfoDialog
import com.cinetrack.ui.components.dialog.ProviderItem

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.data.model.GenreConstants
import com.cinetrack.data.model.SortConfig
import com.cinetrack.ui.utils.ProviderConstants
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.horizontalFadingEdges

@Composable
fun FilterSortSection(
    category: String,
    isCollectionFilter: Boolean,
    isCommentsFilter: Boolean,
    isVisti: Boolean,
    isFolder: Boolean,
    sortConfig: SortConfig,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onInfoClick: () -> Unit,
    onSortConfigChanged: (SortConfig) -> Unit
) {
    ExpandableSection(
        title = stringResource(R.string.filter_sort_by),
        isExpanded = isExpanded,
        showChevron = true,
        isClickable = true,
        onToggle = onToggle
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val isDiscoverCategory = remember(category) {
                category.contains("popular") ||
                    category.contains("upcoming") ||
                    category.contains("now_playing") ||
                    category.contains("airing") ||
                    category.contains("trending") ||
                    category.contains("streaming") ||
                    category.contains("on_the_air") ||
                    category == "genre"
            }

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
                    } else if (category != "movie") {
                        add(FilterOption("last_watched_at", stringResource(R.string.filter_sort_last_watched_at)))
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
                        add(FilterOption("series_status", stringResource(R.string.filter_sort_series_status)))
                        add(FilterOption("remaining_episodes", stringResource(R.string.filter_sort_remaining_episodes)))
                    }
                }
            }

            sortOptions.forEach { option ->
                SortOptionItem(
                    label = option.label,
                    isSelected = sortConfig.sortType == option.id,
                    onInfoClick = if (option.id == "series_status") onInfoClick else null,
                    onClick = {
                        val newDirection = if (sortConfig.sortType != option.id && (option.id == "series_status" || option.id == "title")) "asc" else sortConfig.sortDirection
                        onSortConfigChanged(sortConfig.copy(sortType = option.id, sortDirection = newDirection))
                    }
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
                    isSelected = sortConfig.sortDirection == "desc",
                    icon = ImageVector.vectorResource(id = R.drawable.ic_right),
                    iconRotation = 90f,
                    modifier = Modifier.weight(1f),
                    onClick = { onSortConfigChanged(sortConfig.copy(sortDirection = "desc")) }
                )
                DirectionChip(
                    label = stringResource(R.string.filter_dir_asc),
                    isSelected = sortConfig.sortDirection == "asc",
                    icon = ImageVector.vectorResource(id = R.drawable.ic_right),
                    iconRotation = -90f,
                    modifier = Modifier.weight(1f),
                    onClick = { onSortConfigChanged(sortConfig.copy(sortDirection = "asc")) }
                )
            }
        }
    }
}

@Composable
fun FilterMediaTypeSection(
    sortConfig: SortConfig,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onSortConfigChanged: (SortConfig) -> Unit
) {
    ExpandableSection(
        title = stringResource(R.string.flow_filter_section_media),
        isExpanded = isExpanded,
        showChevron = true,
        isClickable = true,
        badgeCount = if (sortConfig.selectedMedia != null) 1 else 0,
        onToggle = onToggle
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SortOptionItem(
                label = stringResource(R.string.flow_filter_media_all),
                isSelected = sortConfig.selectedMedia == null,
                onClick = { onSortConfigChanged(sortConfig.copy(selectedMedia = null)) }
            )
            SortOptionItem(
                label = stringResource(R.string.flow_filter_media_movies),
                isSelected = sortConfig.selectedMedia == "movie",
                onClick = { onSortConfigChanged(sortConfig.copy(selectedMedia = "movie")) }
            )
            SortOptionItem(
                label = stringResource(R.string.flow_filter_media_tv),
                isSelected = sortConfig.selectedMedia == "tv",
                onClick = { onSortConfigChanged(sortConfig.copy(selectedMedia = "tv")) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterTvStatusSection(
    isVisti: Boolean,
    sortConfig: SortConfig,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onSortConfigChanged: (SortConfig) -> Unit
) {
    val normalizedStatuses = remember(isVisti, sortConfig.selectedStatuses) {
        if (isVisti) {
            sortConfig.selectedStatuses.map { if (it == "up_to_date") "returning" else it }.distinct()
        } else {
            sortConfig.selectedStatuses
        }
    }

    ExpandableSection(
        title = stringResource(R.string.filter_status),
        isExpanded = isExpanded,
        badgeCount = normalizedStatuses.size,
        onToggle = onToggle
    ) {
        val statuses = if (!isVisti) {
            listOf(
                "watching" to stringResource(R.string.filter_status_watching),
                "not_started" to stringResource(R.string.filter_status_not_started),
                "dropped" to stringResource(R.string.filter_status_dropped)
            )
        } else {
            listOf(
                "returning" to stringResource(R.string.status_returning_series),
                "ended" to stringResource(R.string.status_ended),
                "canceled" to stringResource(R.string.status_canceled)
            )
        }

        FlowRow(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            statuses.forEach { (statusId, label) ->
                val isSelected = statusId in normalizedStatuses
                FilterChip(
                    label = label.uppercase(),
                    isSelected = isSelected,
                    onClick = {
                        val newList = if (isSelected) normalizedStatuses - statusId
                        else normalizedStatuses + statusId
                        onSortConfigChanged(sortConfig.copy(selectedStatuses = newList))
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterKeywordsSection(
    suggestedFilters: List<com.cinetrack.ui.viewmodel.FilterPill>,
    initialKeywordName: String?,
    sortConfig: SortConfig,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onSortConfigChanged: (SortConfig) -> Unit
) {
    ExpandableSection(
        title = stringResource(R.string.filter_active_subgenres),
        isExpanded = isExpanded,
        badgeCount = sortConfig.selectedKeywords.size,
        onToggle = onToggle
    ) {
        val currentLanguage = LocalConfiguration.current.locales[0]?.language ?: "en"
        val fallbackLabel = stringResource(R.string.filter_selected_subgenre)

        FlowRow(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sortConfig.selectedKeywords.forEach { keywordId ->
                val dictName = com.cinetrack.data.model.KeywordDictionary.getLocalizedKeywordName(keywordId, currentLanguage)?.uppercase()
                val suggestedName = suggestedFilters.find { it.id == keywordId }?.name?.uppercase()
                val keywordName = suggestedName ?: dictName ?: initialKeywordName?.uppercase() ?: fallbackLabel

                FilterChip(
                    label = keywordName,
                    isSelected = true,
                    onClick = {
                        onSortConfigChanged(sortConfig.copy(selectedKeywords = sortConfig.selectedKeywords - keywordId))
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterGenresSection(
    category: String,
    sortConfig: SortConfig,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onSortConfigChanged: (SortConfig) -> Unit,
    isFolder: Boolean = false,
    hasTvSeries: Boolean = false
) {
    var showAllGenres by remember { mutableStateOf(false) }
    val currentLanguage = LocalConfiguration.current.locales[0]?.language ?: "en"

    ExpandableSection(
        title = stringResource(R.string.filter_genres),
        isExpanded = isExpanded,
        badgeCount = sortConfig.selectedGenres.size,
        onToggle = onToggle
    ) {
        val sortedGenres = remember(category, isFolder, hasTvSeries, currentLanguage) {
            val isTvExclusive = category.contains("tv") && !category.contains("movie") && !isFolder
            val isMovieExclusive = category.contains("movie") && !category.contains("tv") && !isFolder && !hasTvSeries
            val baseGenres = when {
                isTvExclusive -> GenreConstants.TV_GENRES
                isMovieExclusive -> GenreConstants.MOVIE_GENRES
                else -> GenreConstants.ALL_GENRES
            }
            baseGenres.sortedBy { genre ->
                GenreConstants.getLocalizedName(genre.id, currentLanguage, genre.name).lowercase()
            }
        }

        AnimatedContent(
            targetState = showAllGenres,
            transitionSpec = {
                (fadeIn(animationSpec = tween(280)) togetherWith
                        fadeOut(animationSpec = tween(180)))
                    .using(
                        SizeTransform(clip = true) { _, _ ->
                            spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        }
                    )
            },
            label = "GenresExpansion"
        ) { isAllShown ->
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val displayedGenres = if (isAllShown) {
                    sortedGenres
                } else {
                    sortedGenres.filterIndexed { index, genre ->
                        index < 12 || genre.id in sortConfig.selectedGenres
                    }
                }

                displayedGenres.forEach { genre ->
                    val isSelected = genre.id in sortConfig.selectedGenres
                    val localizedName = GenreConstants.getLocalizedName(genre.id, currentLanguage, genre.name)
                    FilterChip(
                        label = localizedName.uppercase(),
                        isSelected = isSelected,
                        onClick = {
                            val newList = if (isSelected) sortConfig.selectedGenres - genre.id
                            else sortConfig.selectedGenres + genre.id
                            onSortConfigChanged(sortConfig.copy(selectedGenres = newList))
                        }
                    )
                }

                if (!isAllShown && sortedGenres.size > displayedGenres.size) {
                    ShowMoreLessToggleChip(
                        isExpanded = false,
                        onClick = { showAllGenres = true }
                    )
                } else if (isAllShown && sortedGenres.size > 12) {
                    ShowMoreLessToggleChip(
                        isExpanded = true,
                        onClick = { showAllGenres = false }
                    )
                }
            }
        }
    }
}

@Composable
fun FilterPlatformsSection(
    sortConfig: SortConfig,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onSortConfigChanged: (SortConfig) -> Unit
) {
    val configuration = LocalConfiguration.current
    ExpandableSection(
        title = stringResource(R.string.filter_platforms),
        isExpanded = isExpanded,
        badgeCount = sortConfig.selectedProviders.size,
        onToggle = onToggle
    ) {
        val cachedProviders by ProviderConstants.cachedProvidersFlow.collectAsStateWithLifecycle()
        val locale = configuration.locales[0] ?: java.util.Locale.getDefault()
        val watchRegion = remember(locale) { com.cinetrack.util.RegionResolver.resolveFromLocale(locale) }
        val availableProviders = remember(watchRegion, cachedProviders) {
            ProviderConstants.getAvailableProvidersForRegion(watchRegion)
        }
        val providersListState = androidx.compose.foundation.lazy.rememberLazyListState()
        LazyRow(
            state = providersListState,
            modifier = Modifier.horizontalFadingEdges(providersListState, leftEdgeWidth = 16.dp, rightEdgeWidth = 16.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(availableProviders, key = { it.providerId }, contentType = { "provider" }) { provider ->
                val isSelected = provider.providerId in sortConfig.selectedProviders
                ProviderItem(
                    name = provider.providerName,
                    logoPath = provider.logoPath,
                    isSelected = isSelected,
                    onClick = {
                        val newList = if (isSelected)
                            sortConfig.selectedProviders - provider.providerId
                        else
                            sortConfig.selectedProviders + provider.providerId
                        onSortConfigChanged(sortConfig.copy(selectedProviders = newList))
                    }
                )
            }
        }
    }
}

